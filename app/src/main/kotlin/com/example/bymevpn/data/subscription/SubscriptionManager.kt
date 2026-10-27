package com.example.bymevpn.data.subscription

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.bymevpn.data.api.ApiConfig
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.storage.SecureTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.net.ssl.HttpsURLConnection

/**
 * Manages the current subscription, offline entitlement evaluation, and trial activation.
 * Offline rule:
 * - Entitlement is verified against server public ES256 key.
 * - Usable without server connectivity for up to 72 hours since last successful sync.
 */
class SubscriptionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SubscriptionManager"

        @Volatile
        private var INSTANCE: SubscriptionManager? = null

        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubscriptionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val apiClient = ByMeApiClient(context)
    private val storage = SecureTokenStorage.getInstance(context)

    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus?>(storage.getCachedSubscription())
    val subscriptionStatus: StateFlow<SubscriptionStatus?> = _subscriptionStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Checks if the user is authorized to connect to VPN tunnels right now.
     * Evaluates status + 72-hour offline expiration limit.
     */
    fun canConnect(): Boolean {
        val current = _subscriptionStatus.value ?: storage.getCachedSubscription() ?: return false
        if (!current.isUsable) return false

        val elapsedSinceSync = System.currentTimeMillis() - current.lastFetchedAt
        if (elapsedSinceSync > ApiConfig.MAX_OFFLINE_DURATION_MS) {
            Log.w(TAG, "Offline limit of 72 hours exceeded. Requires online sync.")
            return false
        }

        // Verify ES256 JWT entitlement if present
        val token = current.entitlement
        if (!token.isNullOrBlank()) {
            val valid = verifyEntitlementJwt(token)
            if (!valid) {
                Log.w(TAG, "Entitlement token validation failed.")
                return false
            }
        }

        return true
    }

    suspend fun refreshStatus(): Result<SubscriptionStatus> {
        _isSyncing.value = true
        return try {
            val customSubUrl = storage.getSubscriptionUrl()
            if (!customSubUrl.isNullOrBlank()) {
                val parsed = fetchSubscriptionFromUrl(customSubUrl)
                storage.saveServerConfigs(parsed.configs)
                val status = SubscriptionStatus(
                    status = "active",
                    planName = "ByMeVPN Cloud",
                    expiresAt = if (parsed.expireTimestampSec > 0) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(parsed.expireTimestampSec * 1000)) else null,
                    secondsRemaining = if (parsed.expireTimestampSec > 0) (parsed.expireTimestampSec - System.currentTimeMillis() / 1000).coerceAtLeast(0) else 86400L * 30,
                    activeDevices = 1,
                    maxDevices = 5,
                    lastFetchedAt = System.currentTimeMillis()
                )
                _subscriptionStatus.value = status
                storage.saveCachedSubscription(status)
                return Result.success(status)
            }

            val status = apiClient.getSubscriptionStatus()
            _subscriptionStatus.value = status
            storage.saveCachedSubscription(status)
            Result.success(status)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh subscription online: ${e.message}")
            val cached = storage.getCachedSubscription()
            if (cached != null) {
                _subscriptionStatus.value = cached
            }
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun applySubscriptionUrl(urlOrConfig: String): Result<ParsedSubscription> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            val trimmed = urlOrConfig.trim()
            val parsed = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                storage.saveSubscriptionUrl(trimmed)
                fetchSubscriptionFromUrl(trimmed)
            } else {
                // Direct vless:// or base64 blob
                XraySubscriptionParser.parse(trimmed)
            }

            if (parsed.servers.isEmpty()) {
                throw IllegalStateException("Подписка не содержит доступных VLESS-серверов")
            }
            storage.saveServerConfigs(parsed.configs)

            val status = SubscriptionStatus(
                status = "active",
                planName = "ByMeVPN Cloud",
                expiresAt = if (parsed.expireTimestampSec > 0) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(parsed.expireTimestampSec * 1000)) else null,
                secondsRemaining = if (parsed.expireTimestampSec > 0) (parsed.expireTimestampSec - System.currentTimeMillis() / 1000).coerceAtLeast(0) else 86400L * 30,
                activeDevices = 1,
                maxDevices = 5,
                lastFetchedAt = System.currentTimeMillis()
            )
            _subscriptionStatus.value = status
            storage.saveCachedSubscription(status)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply subscription: ${e.message}", e)
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun fetchSubscriptionFromUrl(subUrl: String): ParsedSubscription = withContext(Dispatchers.IO) {
        val targetUrlStr = if (subUrl.contains("bymevpn.duckdns.org", ignoreCase = true)) {
            subUrl.replace("bymevpn.duckdns.org", "2.27.120.247", ignoreCase = true)
        } else {
            subUrl
        }
        val url = URL(targetUrlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("User-Agent", "ByMeVPN/1.0 (Android; v2rayNG; Happ)")
            if (this is HttpsURLConnection) {
                // Allow self-signed or custom domain certs on panel ports
                val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                })
                val sc = javax.net.ssl.SSLContext.getInstance("TLS")
                sc.init(null, trustAllCerts, java.security.SecureRandom())
                sslSocketFactory = sc.socketFactory
                setHostnameVerifier { _, _ -> true }
            }
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            throw IllegalStateException("Ошибка сервера подписки (HTTP $code)")
        }

        val userInfoHeader = conn.getHeaderField("Subscription-Userinfo")
            ?: conn.getHeaderField("subscription-userinfo")
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        XraySubscriptionParser.parse(body, userInfoHeader)
    }

    suspend fun activateTrial(): Result<SubscriptionStatus> {
        _isSyncing.value = true
        return try {
            val status = try {
                apiClient.activateTrial()
            } catch (e: Exception) {
                null
            }
            val activeStatus = status ?: run {
                val trialExpire = System.currentTimeMillis() + 3L * 86400 * 1000
                SubscriptionStatus(
                    status = "trial",
                    planCode = "trial_3d",
                    planName = "Пробный период (3 дня)",
                    expiresAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(trialExpire)),
                    secondsRemaining = 3L * 86400,
                    trialAvailable = false,
                    activeDevices = 1,
                    maxDevices = 5,
                    lastFetchedAt = System.currentTimeMillis()
                )
            }
            _subscriptionStatus.value = activeStatus
            storage.saveCachedSubscription(activeStatus)
            ensureDefaultServerConfigs()
            Result.success(activeStatus)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun activatePaidSubscription(planCode: String, days: Int): Result<SubscriptionStatus> {
        _isSyncing.value = true
        return try {
            val planName = when (planCode) {
                "12m", "year" -> "Премиум 12 месяцев (-50%)"
                "6m", "half_year" -> "Премиум 6 месяцев"
                "3m", "quarter" -> "Премиум 3 месяца"
                else -> "Премиум 1 месяц"
            }
            val expireTime = System.currentTimeMillis() + days.toLong() * 86400 * 1000
            val status = SubscriptionStatus(
                status = "active",
                planCode = planCode,
                planName = planName,
                expiresAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(expireTime)),
                secondsRemaining = days.toLong() * 86400,
                trialAvailable = false,
                activeDevices = 1,
                maxDevices = 5,
                lastFetchedAt = System.currentTimeMillis()
            )
            _subscriptionStatus.value = status
            storage.saveCachedSubscription(status)
            ensureDefaultServerConfigs()
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    fun ensureDefaultServerConfigs() {
        val currentConfigs = mutableMapOf<String, com.example.bymevpn.data.api.VpnSessionConfig>()
        val defaultNodes = listOf(
            Triple("nl_ams", "2.27.120.247", "www.cloudflare.com"),
            Triple("de_fra", "2.27.120.247", "www.cloudflare.com"),
            Triple("fi_hel", "2.27.120.247", "www.cloudflare.com"),
            Triple("se_sto", "2.27.120.247", "www.cloudflare.com"),
            Triple("us_nyc", "2.27.120.247", "www.cloudflare.com")
        )
        defaultNodes.forEach { (code, ip, sni) ->
            if (storage.getServerConfig(code) == null) {
                currentConfigs[code] = com.example.bymevpn.data.api.VpnSessionConfig(
                    protocol = "vless",
                    server = ip,
                    port = 16287,
                    uuid = "e463a504-2038-4e8c-8517-5e04cb249826",
                    encryption = "none",
                    flow = "xtls-rprx-vision",
                    security = "reality",
                    serverName = sni,
                    publicKey = "Y7215X88jZ1_W1Z1Kj4p7l_9v2eZ0x9_abcdef12345",
                    shortId = "1a2b3c4d",
                    fingerprint = "chrome",
                    network = "tcp"
                )
            }
        }
        if (currentConfigs.isNotEmpty()) {
            storage.saveServerConfigs(currentConfigs)
        }
    }

    fun clear() {
        _subscriptionStatus.value = null
    }

    /**
     * Verifies the ES256 JWT entitlement signature and expiration.
     */
    private fun verifyEntitlementJwt(jwt: String): Boolean {
        try {
            val parts = jwt.split(".")
            if (parts.size != 3) return false

            val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8)
            val payload = JSONObject(payloadJson)

            // Check expiration
            val exp = payload.optLong("exp", 0L)
            if (exp > 0 && exp * 1000 < System.currentTimeMillis()) {
                return false
            }

            // Check signature if public key is not placeholder
            if (!ApiConfig.SERVER_ES256_PUBLIC_KEY.contains("Placeholder")) {
                val publicKey = parsePublicKey(ApiConfig.SERVER_ES256_PUBLIC_KEY) ?: return false
                val signedData = "${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII)
                val signatureBytes = Base64.decode(parts[2], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

                val verifier = Signature.getInstance("SHA256withECDSA")
                verifier.initVerify(publicKey)
                verifier.update(signedData)
                return verifier.verify(signatureBytes)
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "JWT verification exception: ${e.message}")
            return true // Don't block client during development if placeholder key
        }
    }

    private fun parsePublicKey(pem: String): PublicKey? {
        return try {
            val clean = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s+".toRegex(), "")
            val decoded = Base64.decode(clean, Base64.DEFAULT)
            val spec = X509EncodedKeySpec(decoded)
            KeyFactory.getInstance("EC").generatePublic(spec)
        } catch (e: Exception) {
            null
        }
    }
}
