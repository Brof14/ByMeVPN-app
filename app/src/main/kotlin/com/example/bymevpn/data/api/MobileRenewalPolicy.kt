package com.example.bymevpn.data.api

import org.json.JSONObject

/** Strict parser: absent/invalid renewal data must not be treated as permission to cancel. */
internal object MobileRenewalPolicy {
    data class Status(val autoRenew: Boolean, val canCancel: Boolean)

    fun parse(json: JSONObject): Status {
        require(json.optString("source") == "server") { "Billing status is not server verified" }
        require(json.has("auto_renew") && json.opt("auto_renew") is Boolean) {
            "Server did not provide a confirmed renewal state"
        }
        val enabled = json.getBoolean("auto_renew")
        return Status(
            autoRenew = enabled,
            canCancel = enabled && json.optBoolean("can_cancel", false)
        )
    }
}
