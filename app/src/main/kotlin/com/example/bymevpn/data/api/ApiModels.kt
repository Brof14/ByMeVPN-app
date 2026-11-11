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

enum class TelegramAuthStatus(val wireValue: String) {
    PENDING("pending"),
    AUTHENTICATED("authenticated"),
    EXPIRED("expired");

    companion object {
        fun parse(value: String?): TelegramAuthStatus = when (value?.trim()?.lowercase()) {
            "pending" -> PENDING
            "authenticated" -> AUTHENTICATED
            "expired" -> EXPIRED
            else -> throw ApiError("INVALID_AUTH_RESPONSE", "Telegram authentication returned an invalid session status")
        }
    }
}

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
    /** Short-lived proof used only to bootstrap the first panel HWID registration. */
    val entitlementBootstrap: Boolean = false,
    val entitlementExpiresAt: Long? = null,
    val serverTime: String? = null,
    val subscriptionUrl: String? = null,
    val lastFetchedAt: Long = System.currentTimeMillis()
) {
    val isUsable: Boolean
        get() = status == "active" || status == "trial" || status == "grace"
    val isActive: Boolean
        get() = isUsable
    /** The backend could not determine access; this is not a confirmed missing subscription. */
    val isUndetermined: Boolean
        get() = status == "unknown"
    fun daysRemainingAt(nowMillis: Long): Long? {
        if (secondsRemaining <= 0) return null
        // A slightly slower wall clock right after a fetch must not hide the remaining
        // duration: treat a negative elapsed time as "just synced".
        val elapsedSeconds = ((nowMillis - lastFetchedAt).coerceAtLeast(0L)) / 1000
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
    val discountPercent: Int = 0,
    val priceRub: Int = 0,
    val days: Int = 0,
    val devices: Int = 2,
    /** Explicit backend capability; missing means checkout is unavailable. */
    val purchaseEnabled: Boolean = false
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
    val transportMode: String = "",
    val alpn: String = ""
)

data class DeviceItem(
    val id: String,
    val deviceModel: String,
    val platform: String,
    val lastActive: String,
    val isCurrent: Boolean = false
)

object ApiJsonParsers {
    /** Merge the signed access proof returned by the authenticated /servers handshake. */
    fun mergeServerAccessProof(
        current: SubscriptionStatus,
        payload: JSONObject,
        nowMillis: Long = System.currentTimeMillis()
    ): SubscriptionStatus? {
        val token = payload.optString("entitlement").takeIf { it.isNotBlank() } ?: return null
        val bootstrap = payload.optBoolean("entitlement_bootstrap", false)
        val bootstrapExpiry = payload.optLong("entitlement_expires_at", 0L).takeIf { it > 0L }
        if (bootstrap && bootstrapExpiry == null) return null

        val remaining = if (bootstrap) {
            val bootstrapSeconds = (bootstrapExpiry!! - nowMillis / 1000L).coerceAtLeast(0L)
            if (current.secondsRemaining > 0L) minOf(current.secondsRemaining, bootstrapSeconds) else bootstrapSeconds
        } else current.secondsRemaining
        return current.copy(
            secondsRemaining = remaining,
            entitlement = token,
            entitlementBootstrap = bootstrap,
            entitlementExpiresAt = bootstrapExpiry,
            lastFetchedAt = nowMillis
        )
    }

    fun parsePlans(json: JSONObject): List<PlanItem> {
        val array = json.optJSONArray("raw_array") ?: json.optJSONArray("plans") ?: runCatching {
            json.optString("raw").takeIf { it.isNotBlank() }?.let { JSONArray(it) }
        }.getOrNull() ?: JSONArray()
        val checkoutEnabled = json.optBoolean("payment_enabled", false)
        return buildList {
            for (i in 0 until array.length()) {
                val plan = array.optJSONObject(i) ?: continue
                val code = plan.optString("code", "")
                val priceRub = plan.optInt("price_rub", 0)
                val days = plan.optInt("days", 0)
                val period = when {
                    days in 1..30 -> "1 месяц"
                    days in 31..120 -> "4 месяца"
                    days in 121..240 -> "8 месяцев"
                    days in 241..450 -> "15 месяцев"
                    days > 450 -> "$days дней"
                    else -> ""
                }
                add(
                    PlanItem(
                        code = code,
                        name = plan.optString("name", code),
                        price = if (priceRub > 0) "$priceRub ₽" else plan.optString("price", ""),
                        period = period,
                        isPopular = plan.optBoolean("is_popular", false),
                        discountPercent = plan.optInt("discount_percent", 0),
                        priceRub = priceRub,
                        days = days,
                        devices = plan.optInt("devices", 2),
                        purchaseEnabled = plan.optBoolean("purchase_enabled", checkoutEnabled)
                    )
                )
            }
        }
    }

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

        val planCode = optNullableString("plan_code")
        val explicitDeviceLimit = target.optInt("max_devices", 0).takeIf { it > 0 }
        val deviceLimitFromServerPlan = planCode
            ?.let { Regex("(?:^|_)d(2|5|10)$").find(it)?.groupValues?.get(1)?.toIntOrNull() }

        return SubscriptionStatus(
            status = target.optString("status", "none").trim().lowercase(java.util.Locale.ROOT),
            planCode = planCode,
            planName = optNullableString("plan_name"),
            expiresAt = optNullableString("expires_at"),
            secondsRemaining = target.optLong("seconds_remaining", 0L),
            autoRenew = if (target.has("auto_renew") && !target.isNull("auto_renew")) target.optBoolean("auto_renew") else null,
            trialAvailable = target.optBoolean("trial_available", false),
            maxDevices = explicitDeviceLimit ?: deviceLimitFromServerPlan ?: 0,
            activeDevices = target.optInt("active_devices", 0),
            entitlement = optNullableString("entitlement"),
            entitlementBootstrap = target.optBoolean("entitlement_bootstrap", false),
            entitlementExpiresAt = target.optLong("entitlement_expires_at", 0L).takeIf { it > 0 },
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
            port = json.optInt("port", 0),
            uuid = json.optString("uuid", json.optString("id", "")),
            encryption = json.optString("encryption", "none"),
            flow = json.optString("flow", ""),
            security = json.optString("security", "reality"),
            serverName = json.optString("server_name", json.optString("sni", "")),
            publicKey = json.optString("public_key", json.optString("server_public_key", "")),
            shortId = json.optString("short_id", ""),
            fingerprint = json.optString("fingerprint", "chrome"),
            network = json.optString("network", "tcp"),
            transportPath = json.optString("serviceName", json.optString("service_name", json.optString("path", ""))),
            transportHost = json.optString("host", ""),
            transportMode = json.optString("mode", ""),
            alpn = json.optJSONArray("alpn")?.let { values ->
                (0 until values.length()).map { values.optString(it) }
                    .filter { it.isNotBlank() }.joinToString(",")
            } ?: json.optString("alpn", ""),
            expiresAt = json.optString("expires_at").ifEmpty { null }
        )
    }

    fun parseDevices(array: JSONArray, currentInstallId: String? = null): List<DeviceItem> {
        // Device IDs are server-owned identifiers used by DELETE /devices/{id}.
        // Never synthesize one from the array position: that can target a different
        // device if the server response omits an ID or changes order.
        val byId = linkedMapOf<String, DeviceItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val idStr = sequenceOf("id", "device_id", "hwid_id")
                .map { key -> obj.opt(key)?.toString()?.trim().orEmpty() }
                .firstOrNull { id -> id.isNotBlank() && id != "null" && id.all { it in '0'..'9' } }
                ?: continue
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
            // `is_active` commonly means that a panel client/subscription is active;
            // it does not identify this physical phone.
            val isCurrent = obj.optBoolean("is_current", false) ||
                (!currentInstallId.isNullOrBlank() && installId == currentInstallId)
            val parsed = DeviceItem(
                id = idStr,
                deviceModel = model,
                platform = platform,
                lastActive = lastActive,
                isCurrent = isCurrent
            )
            val previous = byId[idStr]
            byId[idStr] = if (previous == null) parsed else previous.copy(
                isCurrent = previous.isCurrent || parsed.isCurrent,
                lastActive = if (parsed.lastActive.isNotBlank()) parsed.lastActive else previous.lastActive
            )
        }
        return byId.values.toList()
    }
}
