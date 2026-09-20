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
    val isPremium: Boolean = false,
    val countryEn: String = country,
    val cityEn: String = city
) {
    fun localizedCountry(isRu: Boolean): String = if (isRu) country else countryEn
    fun localizedCity(isRu: Boolean): String = if (isRu) city else cityEn
}

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
        val target = json.optJSONObject("user") ?: json
        val idStr = when {
            target.has("user_id") -> target.opt("user_id")?.toString() ?: ""
            target.has("id") -> target.opt("id")?.toString() ?: ""
            else -> ""
        }
        val emailStr = target.optString("email", "")
        val nameRaw = target.opt("name")
        val nameStr = if (nameRaw != null && nameRaw != JSONObject.NULL && nameRaw.toString().isNotBlank() && nameRaw.toString() != "null") {
            nameRaw.toString()
        } else {
            emailStr.substringBefore("@").ifBlank { "User" }
        }
        val createdAtStr = target.optString("created_at").ifEmpty { null }
        return UserProfile(
            id = idStr,
            email = emailStr,
            name = nameStr,
            isGoogle = target.optBoolean("is_google", false),
            createdAt = createdAtStr
        )
    }

    fun parseSubscriptionStatus(json: JSONObject): SubscriptionStatus {
        val target = json.optJSONObject("subscription") ?: json
        fun optNullableString(key: String): String? {
            val v = target.opt(key)
            return if (v != null && v != JSONObject.NULL && v.toString().isNotBlank() && v.toString() != "null") v.toString() else null
        }

        return SubscriptionStatus(
            status = target.optString("status", "none"),
            planCode = optNullableString("plan_code"),
            planName = optNullableString("plan_name"),
            expiresAt = optNullableString("expires_at"),
            secondsRemaining = target.optLong("seconds_remaining", 0L),
            autoRenew = target.optBoolean("auto_renew", false),
            trialAvailable = target.optBoolean("trial_available", false),
            maxDevices = target.optInt("max_devices", 5),
            activeDevices = target.optInt("active_devices", 1),
            entitlement = optNullableString("entitlement"),
            serverTime = optNullableString("server_time"),
            lastFetchedAt = System.currentTimeMillis()
        )
    }

    fun parseAuthResponse(json: JSONObject): AuthResponse {
        val userObj = json.optJSONObject("user") ?: json
        val subObj = json.optJSONObject("subscription")
        return AuthResponse(
            access = json.optString("access", json.optString("access_token", "")),
            refresh = json.optString("refresh", json.optString("refresh_token", "")),
            user = parseUserProfile(userObj),
            subscription = subObj?.let { parseSubscriptionStatus(it) }
        )
    }

    fun parseServers(array: JSONArray): List<ServerNode> {
        val list = mutableListOf<ServerNode>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val code = obj.optString("code", obj.optString("node_code", "default"))
            val cCode = obj.optString("country_code", "NL").uppercase()
            val flagEmoji = when (cCode) {
                "NL" -> "🇳🇱"
                "DE" -> "🇩🇪"
                "US" -> "🇺🇸"
                "FI" -> "🇫🇮"
                "SE" -> "🇸🇪"
                "GB" -> "🇬🇧"
                else -> "🌐"
            }
            val countryRaw = obj.optString("country")
            val countryRu = when {
                countryRaw.isNotBlank() && countryRaw != "Unknown" -> countryRaw
                cCode == "NL" -> "Нидерланды"
                cCode == "DE" -> "Германия"
                cCode == "US" -> "США"
                else -> "Основной сервер"
            }
            val countryEn = when {
                obj.optString("country_en").isNotBlank() -> obj.optString("country_en")
                countryRaw.isNotBlank() && countryRaw != "Unknown" -> countryRaw
                cCode == "NL" -> "Netherlands"
                cCode == "DE" -> "Germany"
                cCode == "US" -> "USA"
                else -> "Main Server"
            }
            val cityRaw = obj.optString("city")
            val cityRu = when {
                cityRaw.isNotBlank() && cityRaw != "Unknown" -> cityRaw
                cCode == "NL" -> "Амстердам"
                cCode == "DE" -> "Франкфурт"
                else -> "Европа"
            }
            val cityEn = when {
                obj.optString("city_en").isNotBlank() -> obj.optString("city_en")
                cityRaw.isNotBlank() && cityRaw != "Unknown" -> cityRaw
                cCode == "NL" -> "Amsterdam"
                cCode == "DE" -> "Frankfurt"
                else -> "Europe"
            }
            val load = obj.optInt("load_pct", obj.optInt("load_percent", 15))

            list.add(
                ServerNode(
                    nodeCode = code,
                    country = countryRu,
                    countryCode = cCode.lowercase(),
                    city = cityRu,
                    flag = obj.optString("flag", flagEmoji),
                    pingMs = obj.optInt("ping_ms", 25),
                    loadPercent = if (load == 0) 18 else load,
                    isPremium = obj.optBoolean("is_premium", false),
                    countryEn = countryEn,
                    cityEn = cityEn
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

    fun parseDevices(array: JSONArray, currentInstallId: String? = null): List<DeviceItem> {
        val list = mutableListOf<DeviceItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val idStr = obj.opt("id")?.toString() ?: i.toString()
            val installId = obj.optString("install_id", "")
            val rawModel = obj.optString("device_model")
            val rawName = obj.optString("device_name")
            val model = when {
                rawModel.isNotBlank() && rawModel != "Unknown" -> rawModel
                rawName.isNotBlank() -> rawName
                else -> "Android Device"
            }
            val platform = obj.optString("platform", "Android")
            val lastActive = obj.optString("last_seen_at", obj.optString("last_active", "Just now"))
            val isCurrent = if (!currentInstallId.isNullOrBlank()) {
                installId == currentInstallId
            } else {
                obj.optBoolean("is_current", obj.optBoolean("is_active", false))
            }
            list.add(
                DeviceItem(
                    id = idStr,
                    deviceModel = model,
                    platform = platform,
                    lastActive = lastActive,
                    isCurrent = isCurrent
                )
            )
        }
        return list
    }
}
