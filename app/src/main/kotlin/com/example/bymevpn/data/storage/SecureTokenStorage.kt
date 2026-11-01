package com.example.bymevpn.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.bymevpn.data.api.DeviceItem
import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.api.UserProfile
import com.example.bymevpn.data.api.ServerNode
import org.json.JSONArray
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
    data class PendingPayment(val planCode: String, val days: Int, val invoiceId: String, val provider: String)

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
        private const val KEY_SUBSCRIPTION_URL = "custom_subscription_url"
        private const val KEY_CUSTOM_CONFIGS = "custom_server_configs_json"
        private const val KEY_SERVER_NODES = "custom_server_nodes_json"
        private const val KEY_API_SERVER_CACHE = "api_server_cache_json"
        private const val KEY_CONFIG_SAVED_AT = "saved_at"
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
        throw IllegalStateException("Secure storage is unavailable", e)
    }

    @Synchronized
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    @Synchronized
    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_CACHED_USER)
            .remove(KEY_CACHED_SUB)
            .remove(KEY_API_SERVER_CACHE)
            .remove(KEY_SUBSCRIPTION_URL)
            .remove(KEY_CUSTOM_CONFIGS)
            .remove(KEY_SERVER_NODES)
            .remove("server_refresh_attempt")
            .remove("registered_devices_json")
            .remove("favorite_servers")
            .remove("dismissed_clipboard_key")
            .remove("pending_payment_plan")
            .remove("pending_payment_days")
            .remove("pending_payment_invoice")
            .remove("pending_payment_provider")
            .remove("pending_payment_time")
            .remove("payment_request_id")
            .remove("payment_request_plan")
            .remove("payment_request_provider")
            .remove("payment_request_devices")
            .remove("payment_request_amount")
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
            put("display_data_version", 1)
        }
        prefs.edit().putString(KEY_CACHED_SUB, obj.toString()).apply()
    }

    fun getCachedSubscription(): SubscriptionStatus? {
        val raw = prefs.getString(KEY_CACHED_SUB, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            // Older clients stored invented duration and device counts for imported subscriptions.
            val confirmedDisplayData = obj.optInt("display_data_version", 0) >= 1
            SubscriptionStatus(
                status = obj.optString("status", "none"),
                planCode = obj.optString("plan_code").ifEmpty { null },
                planName = obj.optString("plan_name").ifEmpty { null },
                expiresAt = obj.optString("expires_at").ifEmpty { null },
                secondsRemaining = if (confirmedDisplayData) obj.optLong("seconds_remaining", 0L) else 0L,
                // Older versions cached an invented value; only trust a fresh server response.
                autoRenew = null,
                trialAvailable = obj.optBoolean("trial_available", false),
                maxDevices = if (confirmedDisplayData) obj.optInt("max_devices", 0) else 0,
                activeDevices = if (confirmedDisplayData) obj.optInt("active_devices", 0) else 0,
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
            // App-scoped pseudonymous identifier, not the cross-app Android hardware ID.
            // Existing saved IDs remain unchanged to avoid unexpectedly re-registering devices.
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_INSTALL_ID, id).apply()
        }
        return id
    }

    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    }

    fun saveSubscriptionUrl(url: String?) {
        if (url.isNullOrBlank()) {
            prefs.edit().remove(KEY_SUBSCRIPTION_URL).apply()
        } else {
            prefs.edit().putString(KEY_SUBSCRIPTION_URL, url.trim()).apply()
        }
    }

    fun getSubscriptionUrl(): String? = prefs.getString(KEY_SUBSCRIPTION_URL, null)

    fun getServerRefreshAttempt(): Long = prefs.getLong("server_refresh_attempt", 0L)
    fun setServerRefreshAttempt(time: Long) { prefs.edit().putLong("server_refresh_attempt", time).apply() }

    fun saveServerConfigs(configs: Map<String, com.example.bymevpn.data.api.VpnSessionConfig>) {
        val root = JSONObject()
        val savedAt = System.currentTimeMillis()
        configs.forEach { (nodeCode, cfg) ->
            root.put(nodeCode, configToJson(cfg, savedAt))
        }
        prefs.edit().putString(KEY_CUSTOM_CONFIGS, root.toString()).apply()
    }

    fun saveServerNodes(nodes: List<ServerNode>) {
        prefs.edit().putString(KEY_SERVER_NODES, serializeServerNodes(nodes).toString()).apply()
    }

    private fun serializeServerNodes(nodes: List<ServerNode>): JSONArray {
        val array = JSONArray()
        nodes.forEach { node ->
            array.put(JSONObject().apply {
                put("nodeCode", node.nodeCode)
                put("country", node.country)
                put("countryCode", node.countryCode)
                put("city", node.city)
                put("countryEn", node.countryEn)
                put("cityEn", node.cityEn)
                put("flag", node.flag)
                put("protocol", node.protocol)
                put("isPremium", node.isPremium)
                put("remark", node.remark)
                put("displayName", node.displayName)
            })
        }
        return array
    }

    private fun parseServerNodes(array: JSONArray): List<ServerNode> = try {
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            ServerNode(nodeCode = obj.getString("nodeCode"), country = obj.optString("country", ""),
                countryCode = obj.optString("countryCode", ""), city = obj.optString("city", ""),
                flag = obj.optString("flag", ""), pingMs = -1, loadPercent = -1,
                countryEn = obj.optString("countryEn", obj.optString("country", "")),
                cityEn = obj.optString("cityEn", obj.optString("city", "")),
                isPremium = obj.optBoolean("isPremium", false), remark = obj.optString("remark", ""),
                protocol = obj.optString("protocol", "VPN"), displayName = obj.optString("displayName", ""))
        }
    } catch (_: Exception) { emptyList() }

    fun getServerNodes(): List<ServerNode> = try {
        parseServerNodes(JSONArray(prefs.getString(KEY_SERVER_NODES, "[]")))
    } catch (_: Exception) { emptyList() }

    data class ApiServerCache(val nodes: List<ServerNode>, val savedAt: Long)

    fun saveApiServerCache(ownerId: String, nodes: List<ServerNode>, savedAt: Long) {
        if (ownerId.isBlank() || nodes.isEmpty()) return
        val cache = JSONObject().put("owner", ownerId).put("savedAt", savedAt)
            .put("nodes", serializeServerNodes(nodes))
        prefs.edit().putString(KEY_API_SERVER_CACHE, cache.toString()).apply()
    }

    fun getApiServerCache(ownerId: String): ApiServerCache? {
        if (ownerId.isBlank()) return null
        return try {
            val cache = JSONObject(prefs.getString(KEY_API_SERVER_CACHE, null) ?: return null)
            if (cache.optString("owner") != ownerId) return null
            val nodes = parseServerNodes(cache.getJSONArray("nodes"))
            if (nodes.isEmpty()) null else ApiServerCache(nodes, cache.optLong("savedAt", 0L))
        } catch (_: Exception) { null }
    }

    fun getServerConfig(nodeCode: String): com.example.bymevpn.data.api.VpnSessionConfig? {
        val raw = prefs.getString(KEY_CUSTOM_CONFIGS, null) ?: return null
        return try {
            val root = JSONObject(raw)
            val obj = root.optJSONObject(nodeCode) ?: return null
            parseFreshServerConfig(obj, System.currentTimeMillis())
        } catch (e: Exception) {
            null
        }
    }

    fun saveServerConfig(nodeCode: String, cfg: com.example.bymevpn.data.api.VpnSessionConfig) {
        val root = try {
            JSONObject(prefs.getString(KEY_CUSTOM_CONFIGS, null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        root.put(nodeCode, configToJson(cfg, System.currentTimeMillis()))
        prefs.edit().putString(KEY_CUSTOM_CONFIGS, root.toString()).apply()
    }

    fun getServerConfigs(): Map<String, com.example.bymevpn.data.api.VpnSessionConfig> {
        val raw = prefs.getString(KEY_CUSTOM_CONFIGS, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val result = mutableMapOf<String, com.example.bymevpn.data.api.VpnSessionConfig>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val nodeCode = keys.next()
                val obj = root.optJSONObject(nodeCode) ?: continue
                val cfg = parseFreshServerConfig(obj, System.currentTimeMillis()) ?: continue
                result[nodeCode] = cfg
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun configToJson(
        cfg: com.example.bymevpn.data.api.VpnSessionConfig,
        savedAt: Long
    ) = JSONObject().apply {
        put("protocol", cfg.protocol)
        put("server", cfg.server)
        put("port", cfg.port)
        put("uuid", cfg.uuid)
        put("encryption", cfg.encryption)
        put("flow", cfg.flow)
        put("security", cfg.security)
        put("serverName", cfg.serverName)
        put("publicKey", cfg.publicKey)
        put("shortId", cfg.shortId)
        put("fingerprint", cfg.fingerprint)
        put("network", cfg.network)
        put("transportPath", cfg.transportPath)
        put("transportHost", cfg.transportHost)
        put("transportMode", cfg.transportMode)
        put(KEY_CONFIG_SAVED_AT, savedAt)
    }

    private fun parseFreshServerConfig(
        obj: JSONObject,
        now: Long
    ): com.example.bymevpn.data.api.VpnSessionConfig? {
        // Legacy cache entries have no timestamp and are intentionally discarded.
        if (!ServerConfigCachePolicy.isFresh(obj.optLong(KEY_CONFIG_SAVED_AT, 0L), now)) return null
        return com.example.bymevpn.data.api.VpnSessionConfig(
            protocol = obj.optString("protocol", "vless"),
            server = obj.optString("server", ""),
            port = obj.optInt("port", 443),
            uuid = obj.optString("uuid", ""),
            encryption = obj.optString("encryption", "none"),
            flow = obj.optString("flow", "xtls-rprx-vision"),
            security = obj.optString("security", "reality"),
            serverName = obj.optString("serverName", ""),
            publicKey = obj.optString("publicKey", ""),
            shortId = obj.optString("shortId", ""),
            fingerprint = obj.optString("fingerprint", "chrome"),
            network = obj.optString("network", "tcp"),
            transportPath = obj.optString("transportPath", ""),
            transportHost = obj.optString("transportHost", ""),
            transportMode = obj.optString("transportMode", "")
        )
    }

    fun getFavoriteServers(): Set<String> {
        return prefs.getStringSet("favorite_servers", emptySet()) ?: emptySet()
    }

    fun toggleFavoriteServer(nodeCode: String) {
        val current = getFavoriteServers().toMutableSet()
        if (current.contains(nodeCode)) {
            current.remove(nodeCode)
        } else {
            current.add(nodeCode)
        }
        prefs.edit().putStringSet("favorite_servers", current).apply()
    }

    fun isFavoriteServer(nodeCode: String): Boolean {
        return getFavoriteServers().contains(nodeCode)
    }

    fun setDismissedClipboardKey(key: String) {
        prefs.edit().putString("dismissed_clipboard_key", key).apply()
    }

    fun getDismissedClipboardKey(): String? {
        return prefs.getString("dismissed_clipboard_key", null)
    }

    fun getRegisteredDevices(): List<DeviceItem> {
        val raw = prefs.getString("registered_devices_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<DeviceItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DeviceItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        deviceModel = obj.optString("device_model", "Android Device"),
                        platform = obj.optString("platform", "Android"),
                        lastActive = obj.optString("last_active", "Just now"),
                        isCurrent = obj.optBoolean("is_current", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRegisteredDevices(devices: List<DeviceItem>) {
        val arr = JSONArray()
        devices.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("device_model", d.deviceModel)
                put("platform", d.platform)
                put("last_active", d.lastActive)
                put("is_current", d.isCurrent)
            }
            arr.put(obj)
        }
        prefs.edit().putString("registered_devices_json", arr.toString()).apply()
    }

    fun removeRegisteredDevice(deviceId: String) {
        val current = getRegisteredDevices().filter { it.id != deviceId }
        saveRegisteredDevices(current)
    }

    fun addRegisteredDevice(device: DeviceItem) {
        val current = getRegisteredDevices().filter { it.id != device.id }.toMutableList()
        current.add(device)
        saveRegisteredDevices(current)
    }

    fun savePendingPayment(planCode: String, days: Int, invoiceId: String, provider: String) {
        prefs.edit()
            .putString("pending_payment_plan", planCode)
            .putInt("pending_payment_days", days)
            .putString("pending_payment_invoice", invoiceId)
            .putString("pending_payment_provider", provider)
            .putLong("pending_payment_time", System.currentTimeMillis())
            .apply()
    }

    fun getOrCreatePaymentRequestId(planCode: String, provider: String, devices: Int = 2, amountRub: Int = 0): String {
        val existing = prefs.getString("payment_request_id", null)
        val existingPlan = prefs.getString("payment_request_plan", null)
        val existingProvider = prefs.getString("payment_request_provider", null)
        val existingDevices = prefs.getInt("payment_request_devices", -1)
        val existingAmount = prefs.getInt("payment_request_amount", -1)
        if (!existing.isNullOrBlank() && existingPlan == planCode && existingProvider == provider && existingDevices == devices && existingAmount == amountRub) return existing
        val created = UUID.randomUUID().toString()
        prefs.edit()
            .putString("payment_request_id", created)
            .putString("payment_request_plan", planCode)
            .putString("payment_request_provider", provider)
            .putInt("payment_request_devices", devices)
            .putInt("payment_request_amount", amountRub)
            .apply()
        return created
    }

    fun getPendingPayment(): PendingPayment? {
        val plan = prefs.getString("pending_payment_plan", null) ?: return null
        val days = prefs.getInt("pending_payment_days", 30)
        val invoiceId = prefs.getString("pending_payment_invoice", null) ?: return null
        val provider = prefs.getString("pending_payment_provider", null) ?: return null
        val time = prefs.getLong("pending_payment_time", 0L)
        // Valid within 24 hours
        if (System.currentTimeMillis() - time > 24 * 3600 * 1000) {
            clearPendingPayment()
            return null
        }
        return PendingPayment(plan, days, invoiceId, provider)
    }

    fun clearPendingPayment() {
        prefs.edit()
            .remove("pending_payment_plan")
            .remove("pending_payment_days")
            .remove("pending_payment_invoice")
            .remove("pending_payment_provider")
            .remove("payment_request_id")
            .remove("payment_request_plan")
            .remove("payment_request_provider")
            .remove("payment_request_devices")
            .remove("payment_request_amount")
            .remove("pending_payment_time")
            .apply()
    }

    fun isVpnDisclosureAccepted(): Boolean = prefs.getBoolean("vpn_prominent_disclosure_accepted", false)

    fun setVpnDisclosureAccepted(accepted: Boolean) {
        prefs.edit().putBoolean("vpn_prominent_disclosure_accepted", accepted).apply()
    }

    fun clearAllUserData() {
        prefs.edit().clear().apply()
    }
}
