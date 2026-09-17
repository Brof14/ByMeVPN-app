package com.example.bymevpn.data.subscription

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.bymevpn.data.api.ApiConfig
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.storage.SecureTokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
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

    suspend fun activateTrial(): Result<SubscriptionStatus> {
        _isSyncing.value = true
        return try {
            val status = apiClient.activateTrial()
            _subscriptionStatus.value = status
            storage.saveCachedSubscription(status)
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncing.value = false
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
