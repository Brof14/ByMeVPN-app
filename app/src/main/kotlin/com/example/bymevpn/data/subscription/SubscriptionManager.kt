package com.example.bymevpn.data.subscription

import android.content.Context
import android.os.Build
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
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages the current subscription, offline entitlement evaluation, and trial activation.
 * Offline rule:
 * - When the release has the server ES256 key, its signed entitlement is mandatory.
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

        val now = System.currentTimeMillis()
        val elapsedSinceSync = now - current.lastFetchedAt
        if (elapsedSinceSync < 0 || elapsedSinceSync > ApiConfig.MAX_OFFLINE_DURATION_MS) {
            Log.w(TAG, "Offline entitlement requires an online sync.")
            return false
        }
        if (current.knownExpiryReached(now)) return false

        // New release builds opt into signed, account/device-bound offline access.
        val token = current.entitlement
        val publicKey = ApiConfig.SERVER_ES256_PUBLIC_KEY
        if (publicKey.isNotBlank() && token.isNullOrBlank()) {
            Log.w(TAG, "Signed subscription confirmation is missing.")
            return false
        }
        if (!token.isNullOrBlank()) {
            val userId = storage.getCachedUser()?.id.orEmpty()
            val hwid = storage.getInstallId(context)
            if (!EntitlementJwtVerifier.verify(token, publicKey, userId, hwid, current)) {
                Log.w(TAG, "Entitlement token validation failed.")
                return false
            }
        }

        return true
    }

    suspend fun refreshStatus(forceBackend: Boolean = false): Result<SubscriptionStatus> = refreshMutex.withLock {
        _isSyncing.value = true
        try {
            val customSubUrl = storage.getSubscriptionUrl()
            if (!forceBackend && !customSubUrl.isNullOrBlank()) {
                val elapsed = System.currentTimeMillis() - storage.getServerRefreshAttempt()
                val cached = storage.getCachedSubscription()
                if (elapsed in 0 until 6L * 60 * 60 * 1000 && cached != null && storage.getServerConfigs().isNotEmpty()) {
                    _subscriptionStatus.value = cached
                    return@withLock Result.success(cached)
                }
                val parsed = fetchSubscriptionFromUrl(customSubUrl)
                require(parsed.servers.isNotEmpty() && parsed.configs.isNotEmpty()) { "Subscription contains no servers" }
                storage.saveServerConfigs(parsed.configs)
                storage.saveServerNodes(parsed.servers)
                storage.setServerRefreshAttempt(System.currentTimeMillis())
                val status = SubscriptionStatus(
                    status = if (parsed.expireTimestampSec > 0 && parsed.expireTimestampSec <= System.currentTimeMillis() / 1000) "expired" else "active",
                    expiresAt = if (parsed.expireTimestampSec > 0) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(parsed.expireTimestampSec * 1000)) else null,
                    secondsRemaining = if (parsed.expireTimestampSec > 0) (parsed.expireTimestampSec - System.currentTimeMillis() / 1000).coerceAtLeast(0) else 0,
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
            Log.w(TAG, "Failed to refresh subscription online: ${e.javaClass.simpleName}")
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
                status = if (parsed.expireTimestampSec > 0 && parsed.expireTimestampSec <= System.currentTimeMillis() / 1000) "expired" else "active",
                expiresAt = if (parsed.expireTimestampSec > 0) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(parsed.expireTimestampSec * 1000)) else null,
                secondsRemaining = if (parsed.expireTimestampSec > 0) (parsed.expireTimestampSec - System.currentTimeMillis() / 1000).coerceAtLeast(0) else 0,
                lastFetchedAt = System.currentTimeMillis()
            )
            _subscriptionStatus.value = status
            storage.saveCachedSubscription(status)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply subscription: ${e.javaClass.simpleName}")
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun fetchSubscriptionFromUrl(subUrl: String): ParsedSubscription = withContext(Dispatchers.IO) {
        val resolved = SubscriptionUrlPolicy.resolve(subUrl, ApiConfig.BASE_URL)
        val url = resolved.url
        if (resolved.rewritten) {
            Log.w(TAG, "Subscription URL was server-local or plain HTTP; retrying over HTTPS on the public backend host")
        }
        require(url.protocol.equals("https", ignoreCase = true)) { "Subscription URL must use HTTPS" }
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
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

        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "Subscription endpoint returned HTTP $code")
                throw IllegalStateException("Ошибка сервера подписки (HTTP $code)")
            }

            val userInfoHeader = conn.getHeaderField("Subscription-Userinfo")
                ?: conn.getHeaderField("subscription-userinfo")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val parsed = XraySubscriptionParser.parse(body, userInfoHeader)
            Log.d(TAG, "Subscription fetched: servers=${parsed.servers.size}, configs=${parsed.configs.size}")
            parsed
        } finally {
            conn.disconnect()
        }
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

}
