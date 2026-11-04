package com.example.bymevpn.data.storage

import com.example.bymevpn.data.api.SubscriptionStatus
import org.json.JSONObject

/** JSON codec for server-confirmed subscription state stored between app launches. */
internal object SubscriptionCacheCodec {
    fun encode(status: SubscriptionStatus): String = JSONObject().apply {
        put("status", status.status)
        put("plan_code", status.planCode ?: "")
        put("plan_name", status.planName ?: "")
        put("expires_at", status.expiresAt ?: "")
        put("seconds_remaining", status.secondsRemaining)
        put("auto_renew", status.autoRenew)
        put("trial_available", status.trialAvailable)
        put("max_devices", status.maxDevices)
        put("active_devices", status.activeDevices)
        put("entitlement", status.entitlement ?: "")
        put("entitlement_bootstrap", status.entitlementBootstrap)
        put("entitlement_expires_at", status.entitlementExpiresAt ?: 0L)
        put("server_time", status.serverTime ?: "")
        put("last_fetched_at", status.lastFetchedAt)
        put("display_data_version", 1)
    }.toString()

    fun decode(raw: String): SubscriptionStatus? = try {
        val json = JSONObject(raw)
        // Older clients cached invented duration and device counts for imported subscriptions.
        val confirmedDisplayData = json.optInt("display_data_version", 0) >= 1
        fun optionalString(key: String): String? = json.optString(key).takeIf { it.isNotBlank() }

        SubscriptionStatus(
            status = json.optString("status", "none"),
            planCode = optionalString("plan_code"),
            planName = optionalString("plan_name"),
            expiresAt = optionalString("expires_at"),
            secondsRemaining = if (confirmedDisplayData) json.optLong("seconds_remaining", 0L) else 0L,
            // Older clients cached an invented value; only trust a fresh server response.
            autoRenew = null,
            trialAvailable = json.optBoolean("trial_available", false),
            maxDevices = if (confirmedDisplayData) json.optInt("max_devices", 0) else 0,
            activeDevices = if (confirmedDisplayData) json.optInt("active_devices", 0) else 0,
            entitlement = optionalString("entitlement"),
            entitlementBootstrap = confirmedDisplayData && json.optBoolean("entitlement_bootstrap", false),
            entitlementExpiresAt = if (confirmedDisplayData) {
                json.optLong("entitlement_expires_at", 0L).takeIf { it > 0L }
            } else null,
            serverTime = optionalString("server_time"),
            lastFetchedAt = json.optLong("last_fetched_at", System.currentTimeMillis())
        )
    } catch (_: Exception) {
        null
    }
}
