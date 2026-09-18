package com.example.bymevpn.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.api.UserProfile
import org.json.JSONObject
import java.util.UUID

/**
 * Secure persistent store using AndroidX EncryptedSharedPreferences (AES-256-GCM).
 * Securely persists:
 * - JWT Access and Refresh tokens
 * - Cached User profile and Subscription status
 * - Stable Install ID for anti-fraud
 */
class SecureTokenStorage private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: SecureTokenStorage? = null

        fun getInstance(context: Context): SecureTokenStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureTokenStorage(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val PREFS_NAME = "bymevpn_secure_vault"
        private const val KEY_ACCESS_TOKEN = "jwt_access_token"
        private const val KEY_REFRESH_TOKEN = "jwt_refresh_token"
        private const val KEY_CACHED_USER = "cached_user_profile"
        private const val KEY_CACHED_SUB = "cached_subscription_status"
        private const val KEY_INSTALL_ID = "app_install_id"
    }

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to standard private preferences if Keystore has device issues
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_CACHED_USER)
            .remove(KEY_CACHED_SUB)
            .apply()
    }

    fun saveCachedUser(user: UserProfile) {
        val obj = JSONObject().apply {
            put("id", user.id)
            put("email", user.email)
            put("name", user.name)
            put("is_google", user.isGoogle)
            put("created_at", user.createdAt ?: "")
        }
        prefs.edit().putString(KEY_CACHED_USER, obj.toString()).apply()
    }

    fun getCachedUser(): UserProfile? {
        val raw = prefs.getString(KEY_CACHED_USER, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            UserProfile(
                id = obj.optString("id", ""),
                email = obj.optString("email", ""),
                name = obj.optString("name", "User"),
                isGoogle = obj.optBoolean("is_google", false),
                createdAt = obj.optString("created_at").ifEmpty { null }
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveCachedSubscription(sub: SubscriptionStatus) {
        val obj = JSONObject().apply {
            put("status", sub.status)
            put("plan_code", sub.planCode ?: "")
            put("plan_name", sub.planName ?: "")
            put("expires_at", sub.expiresAt ?: "")
            put("seconds_remaining", sub.secondsRemaining)
            put("auto_renew", sub.autoRenew)
            put("trial_available", sub.trialAvailable)
            put("max_devices", sub.maxDevices)
            put("active_devices", sub.activeDevices)
            put("entitlement", sub.entitlement ?: "")
            put("server_time", sub.serverTime ?: "")
            put("last_fetched_at", sub.lastFetchedAt)
        }
        prefs.edit().putString(KEY_CACHED_SUB, obj.toString()).apply()
    }

    fun getCachedSubscription(): SubscriptionStatus? {
        val raw = prefs.getString(KEY_CACHED_SUB, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            SubscriptionStatus(
                status = obj.optString("status", "none"),
                planCode = obj.optString("plan_code").ifEmpty { null },
                planName = obj.optString("plan_name").ifEmpty { null },
                expiresAt = obj.optString("expires_at").ifEmpty { null },
                secondsRemaining = obj.optLong("seconds_remaining", 0L),
                autoRenew = obj.optBoolean("auto_renew", false),
                trialAvailable = obj.optBoolean("trial_available", false),
                maxDevices = obj.optInt("max_devices", 5),
                activeDevices = obj.optInt("active_devices", 1),
                entitlement = obj.optString("entitlement").ifEmpty { null },
                serverTime = obj.optString("server_time").ifEmpty { null },
                lastFetchedAt = obj.optLong("last_fetched_at", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stable anonymous installation ID for trial anti-fraud.
     */
    fun getInstallId(context: Context): String {
        var id = prefs.getString(KEY_INSTALL_ID, null)
        if (id.isNullOrBlank()) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                null
            }
            id = androidId?.ifBlank { null } ?: UUID.randomUUID().toString()
            prefs.edit().putString(KEY_INSTALL_ID, id).apply()
        }
        return id
    }

    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    }
}
