package com.example.bymevpn.data.api

import org.json.JSONArray
import org.json.JSONObject

data class AuthResponse(
    val access: String,
    val refresh: String,
    val user: UserProfile,
    val subscription: SubscriptionStatus?
)

data class TelegramSessionResponse(
    val sessionToken: String,
    val botUrl: String
)

data class TelegramCheckResult(
    val status: String,
    val auth: AuthResponse? = null
)

data class PaymentCreateResponse(
    val paymentUrl: String,
    val provider: String,
    val invoiceId: String? = null
)

data class PaymentCheckResult(
    val status: String,
    val isActive: Boolean,
    val subscription: SubscriptionStatus? = null
)

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val isGoogle: Boolean = false,
    val createdAt: String? = null
) {
    val displayName: String get() = name
}

data class SubscriptionStatus(
    val status: String, // none | trial | active | expired | grace
    val planCode: String? = null,
    val planName: String? = null,
    val expiresAt: String? = null,
    val secondsRemaining: Long = 0L,
    val autoRenew: Boolean? = null,
    val trialAvailable: Boolean = false,
    val maxDevices: Int = 0,
    val activeDevices: Int = 0,
    val entitlement: String? = null,
    val serverTime: String? = null,
    val subscriptionUrl: String? = null,
    val lastFetchedAt: Long = System.currentTimeMillis()
) {
    val isUsable: Boolean
        get() = status == "active" || status == "trial" || status == "grace"
    val isActive: Boolean
        get() = isUsable
    fun daysRemainingAt(nowMillis: Long): Long? {
        if (secondsRemaining <= 0 || nowMillis < lastFetchedAt) return null
        val elapsedSeconds = (nowMillis - lastFetchedAt) / 1000
        val remaining = (secondsRemaining - elapsedSeconds).coerceAtLeast(0)
        return if (remaining > 0) (remaining - 1) / 86400 + 1 else null
    }
    fun knownExpiryReached(nowMillis: Long): Boolean =
        secondsRemaining > 0 && nowMillis >= lastFetchedAt &&
            (nowMillis - lastFetchedAt) / 1000 >= secondsRemaining
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
    val city: String = "",
    val flag: String = "🌐",
    val pingMs: Int = -1,
    val loadPercent: Int = -1,
    val isPremium: Boolean = false,
    val countryEn: String = country,
    val cityEn: String = city,
    val protocol: String = "VPN",
    val remark: String = "",
    val displayName: String = "",
    val connectionCheck: Int = 0
) {
    val id: String get() = nodeCode
    val name: String get() {
        if (displayName.isNotBlank()) return displayName
        if (remark.isNotBlank()) return "$remark • $protocol"
        val cityPart = if (city.isNotBlank()) " $city" else ""
        return "$country$cityPart • $protocol"
    }
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
    val expiresAt: String? = null,
    val transportPath: String = "",
    val transportHost: String = "",
    val transportMode: String = ""
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
            autoRenew = if (target.has("auto_renew") && !target.isNull("auto_renew")) target.optBoolean("auto_renew") else null,
            trialAvailable = target.optBoolean("trial_available", false),
            maxDevices = target.optInt("max_devices", 0),
            activeDevices = target.optInt("active_devices", 0),
            entitlement = optNullableString("entitlement"),
            serverTime = optNullableString("server_time"),
            subscriptionUrl = optNullableString("subscription_url") ?: optNullableString("sub_url"),
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
            val cCode = obj.optString("country_code", "").uppercase()
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
                else -> ""
            }
            val cityEn = when {
                obj.optString("city_en").isNotBlank() -> obj.optString("city_en")
                cityRaw.isNotBlank() && cityRaw != "Unknown" -> cityRaw
                else -> ""
            }
            val load = obj.optInt("load_pct", obj.optInt("load_percent", -1))
            val proto = obj.optString("protocol").ifBlank {
                val cfg = obj.optJSONObject("config")
                val p = cfg?.optString("protocol", "") ?: ""
                val sec = cfg?.optString("security", "") ?: ""
                if (p.equals("hysteria2", true) || p.equals("hy2", true)) "Hysteria 2"
                else if (p.equals("vless", true) && sec.equals("reality", true)) "VLESS Reality"
                else if (p.equals("vless", true)) "VLESS"
                else "VPN"
            }
            val remarkStr = obj.optString("remark", "")
            val displayNameStr = obj.optString("display_name", "")

            list.add(
                ServerNode(
                    nodeCode = code,
                    country = countryRu,
                    countryCode = cCode.lowercase(),
                    city = cityRu,
                    flag = obj.optString("flag", flagEmoji),
                    pingMs = -1,
                    loadPercent = load,
                    isPremium = obj.optBoolean("is_premium", false),
                    countryEn = countryEn,
                    cityEn = cityEn,
                    protocol = proto,
                    remark = remarkStr,
                    displayName = displayNameStr
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
            transportPath = json.optString("path", ""),
            transportHost = json.optString("host", ""),
            transportMode = json.optString("mode", ""),
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
            val isCurrent = obj.optBoolean("is_current", false) || obj.optBoolean("is_active", false) ||
                (!currentInstallId.isNullOrBlank() && installId == currentInstallId)
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
