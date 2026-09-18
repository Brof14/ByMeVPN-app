package com.example.bymevpn.data.api

import org.json.JSONArray
import org.json.JSONObject

data class AuthResponse(
    val access: String,
    val refresh: String,
    val user: UserProfile,
    val subscription: SubscriptionStatus?
)

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val isGoogle: Boolean = false,
    val createdAt: String? = null
)

data class SubscriptionStatus(
    val status: String, // none | trial | active | expired | grace
    val planCode: String? = null,
    val planName: String? = null,
    val expiresAt: String? = null,
    val secondsRemaining: Long = 0L,
    val autoRenew: Boolean = false,
    val trialAvailable: Boolean = false,
    val maxDevices: Int = 5,
    val activeDevices: Int = 1,
    val entitlement: String? = null,
    val serverTime: String? = null,
    val lastFetchedAt: Long = System.currentTimeMillis()
) {
    val isUsable: Boolean
        get() = status == "active" || status == "trial" || status == "grace"
}

data class PlanItem(
    val code: String,
    val name: String,
    val price: String,
    val period: String,
    val isPopular: Boolean = false,
    val discountPercent: Int = 0
)

data class ServerNode(
    val nodeCode: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val flag: String,
    val pingMs: Int = 25,
    val loadPercent: Int = 30,
    val isPremium: Boolean = false
)

data class VpnSessionConfig(
    val protocol: String = "vless",
    val server: String,
    val port: Int,
    val uuid: String,
    val encryption: String = "none",
    val flow: String = "xtls-rprx-vision",
    val security: String = "reality",
    val serverName: String,
    val publicKey: String,
    val shortId: String = "",
    val fingerprint: String = "chrome",
    val network: String = "tcp",
    val expiresAt: String? = null
)

data class DeviceItem(
    val id: String,
    val deviceModel: String,
    val platform: String,
    val lastActive: String,
    val isCurrent: Boolean = false
)

object ApiJsonParsers {
    fun parseUserProfile(json: JSONObject): UserProfile {
        return UserProfile(
            id = json.optString("id", ""),
            email = json.optString("email", ""),
            name = json.optString("name", "User"),
            isGoogle = json.optBoolean("is_google", false),
            createdAt = json.optString("created_at").ifEmpty { null }
        )
    }

    fun parseSubscriptionStatus(json: JSONObject): SubscriptionStatus {
        return SubscriptionStatus(
            status = json.optString("status", "none"),
            planCode = json.optString("plan_code").ifEmpty { null },
            planName = json.optString("plan_name").ifEmpty { null },
            expiresAt = json.optString("expires_at").ifEmpty { null },
            secondsRemaining = json.optLong("seconds_remaining", 0L),
            autoRenew = json.optBoolean("auto_renew", false),
            trialAvailable = json.optBoolean("trial_available", false),
            maxDevices = json.optInt("max_devices", 5),
            activeDevices = json.optInt("active_devices", 1),
            entitlement = json.optString("entitlement").ifEmpty { null },
            serverTime = json.optString("server_time").ifEmpty { null },
            lastFetchedAt = System.currentTimeMillis()
        )
    }

    fun parseAuthResponse(json: JSONObject): AuthResponse {
        val userObj = json.getJSONObject("user")
        val subObj = json.optJSONObject("subscription")
        return AuthResponse(
            access = json.getString("access"),
            refresh = json.getString("refresh"),
            user = parseUserProfile(userObj),
            subscription = subObj?.let { parseSubscriptionStatus(it) }
        )
    }

    fun parseServers(array: JSONArray): List<ServerNode> {
        val list = mutableListOf<ServerNode>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ServerNode(
                    nodeCode = obj.getString("node_code"),
                    country = obj.optString("country", "Global"),
                    countryCode = obj.optString("country_code", "NL"),
                    city = obj.optString("city", ""),
                    flag = obj.optString("flag", "🌐"),
                    pingMs = obj.optInt("ping_ms", 25),
                    loadPercent = obj.optInt("load_percent", 35),
                    isPremium = obj.optBoolean("is_premium", false)
                )
            )
        }
        return list
    }

    fun parseVpnSessionConfig(json: JSONObject): VpnSessionConfig {
        return VpnSessionConfig(
            protocol = json.optString("protocol", "vless"),
            server = json.optString("server", json.optString("endpoint", "")),
            port = json.optInt("port", 443),
            uuid = json.optString("uuid", json.optString("id", "")),
            encryption = json.optString("encryption", "none"),
            flow = json.optString("flow", "xtls-rprx-vision"),
            security = json.optString("security", "reality"),
            serverName = json.optString("server_name", json.optString("sni", "")),
            publicKey = json.optString("public_key", json.optString("server_public_key", "")),
            shortId = json.optString("short_id", ""),
            fingerprint = json.optString("fingerprint", "chrome"),
            network = json.optString("network", "tcp"),
            expiresAt = json.optString("expires_at").ifEmpty { null }
        )
    }

    fun parseDevices(array: JSONArray): List<DeviceItem> {
        val list = mutableListOf<DeviceItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                DeviceItem(
                    id = obj.getString("id"),
                    deviceModel = obj.optString("device_model", "Android Device"),
                    platform = obj.optString("platform", "Android"),
                    lastActive = obj.optString("last_active", "Just now"),
                    isCurrent = obj.optBoolean("is_current", false)
                )
            )
        }
        return list
    }
}
