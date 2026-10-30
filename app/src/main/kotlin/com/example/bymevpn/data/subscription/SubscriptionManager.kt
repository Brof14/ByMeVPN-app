package com.example.bymevpn.data.subscription

import android.content.Context
import android.os.Build
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

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

    private val refreshMutex = Mutex()
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

    suspend fun refreshStatus(): Result<SubscriptionStatus> = refreshMutex.withLock {
        _isSyncing.value = true
        try {
            val customSubUrl = storage.getSubscriptionUrl()
            if (!customSubUrl.isNullOrBlank()) {
                val elapsed = System.currentTimeMillis() - storage.getServerRefreshAttempt()
                val cached = storage.getCachedSubscription()
                if (elapsed in 0 until 6L * 60 * 60 * 1000 && cached != null && storage.getServerConfigs().isNotEmpty()) {
                    return@withLock Result.success(cached)
                }
                storage.setServerRefreshAttempt(System.currentTimeMillis())
                val parsed = fetchSubscriptionFromUrl(customSubUrl)
                require(parsed.servers.isNotEmpty() && parsed.configs.isNotEmpty()) { "Subscription contains no servers" }
                storage.saveServerConfigs(parsed.configs)
                storage.saveServerNodes(parsed.servers)
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
                return@withLock Result.success(status)
            }

            val status = apiClient.getSubscriptionStatus()
            _subscriptionStatus.value = status
            storage.saveCachedSubscription(status)
            Result.success(status)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh subscription online")
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
                fetchSubscriptionFromUrl(trimmed)
            } else {
                // Direct vless:// or base64 blob
                XraySubscriptionParser.parse(trimmed)
            }

            if (parsed.servers.isEmpty()) {
                throw IllegalStateException("Подписка не содержит доступных VLESS-серверов")
            }
            require(parsed.configs.isNotEmpty()) { "Subscription contains no configurations" }
            storage.saveServerConfigs(parsed.configs)
            storage.saveServerNodes(parsed.servers)
            if (trimmed.startsWith("https://", ignoreCase = true)) {
                storage.saveSubscriptionUrl(trimmed)
                storage.setServerRefreshAttempt(System.currentTimeMillis())
            }

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
            Log.e(TAG, "Failed to apply subscription")
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun fetchSubscriptionFromUrl(subUrl: String): ParsedSubscription = withContext(Dispatchers.IO) {
        val url = URL(subUrl)
        require(url.protocol.equals("https", ignoreCase = true)) { "Subscription URL must use HTTPS" }
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("User-Agent", "ByMeVPN/1.0 (Android; v2rayNG; Happ)")
            // 3x-ui enforces the subscription's HWID device limit on fetch.
            // Without these headers it intentionally returns HTTP 404.
            setRequestProperty("X-HWID", storage.getInstallId(context))
            setRequestProperty("X-Device-OS", "Android")
            setRequestProperty("X-Ver-OS", Build.VERSION.RELEASE.orEmpty())
            setRequestProperty("X-Device-Model", storage.getDeviceModel())
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
            val activeStatus = apiClient.activateTrial()
            _subscriptionStatus.value = activeStatus
            storage.saveCachedSubscription(activeStatus)
            Result.success(activeStatus)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun activatePaidSubscription(planCode: String, days: Int): Result<SubscriptionStatus> {
        return refreshStatus()
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

            val headerJson = String(Base64.decode(parts[0], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8)
            if (JSONObject(headerJson).optString("alg") != "ES256") return false

            val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8)
            val payload = JSONObject(payloadJson)

            // Check expiration
            val exp = payload.optLong("exp", 0L)
            if (exp <= 0 || exp <= System.currentTimeMillis() / 1000) {
                return false
            }

            // Check signature if public key is not placeholder
            if (!ApiConfig.SERVER_ES256_PUBLIC_KEY.contains("Placeholder")) {
                val publicKey = parsePublicKey(ApiConfig.SERVER_ES256_PUBLIC_KEY) ?: return false
                val signedData = "${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII)
                val rawSignature = Base64.decode(parts[2], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                if (rawSignature.size != 64) return false
                fun derInteger(bytes: ByteArray): ByteArray {
                    val trimmed = bytes.dropWhile { it == 0.toByte() }.toByteArray()
                    val unsigned = if (trimmed.isEmpty()) byteArrayOf(0) else trimmed
                    val value = if ((unsigned[0].toInt() and 0x80) != 0) byteArrayOf(0) + unsigned else unsigned
                    return byteArrayOf(2, value.size.toByte()) + value
                }
                val values = derInteger(rawSignature.copyOfRange(0, 32)) + derInteger(rawSignature.copyOfRange(32, 64))
                val signatureBytes = byteArrayOf(0x30, values.size.toByte()) + values

                val verifier = Signature.getInstance("SHA256withECDSA")
                verifier.initVerify(publicKey)
                verifier.update(signedData)
                return verifier.verify(signatureBytes)
            }

            return false
        } catch (e: Exception) {
            Log.e(TAG, "JWT verification exception: ${e.message}")
            return false
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
