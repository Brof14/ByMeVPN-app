package com.example.bymevpn.data.subscription

import com.example.bymevpn.data.api.SubscriptionStatus

/** Local bounds for a server-signed, short first-device registration window. */
internal object DeviceBootstrapPolicy {
    private const val MAX_TTL_SECONDS = 10 * 60L
    private const val CLOCK_SKEW_SECONDS = 60L

    fun isValid(status: SubscriptionStatus, nowSeconds: Long): Boolean {
        if (!status.entitlementBootstrap || status.status !in setOf("active", "trial", "grace")) return false
        if (status.maxDevices !in setOf(2, 5, 10)) return false
        if (status.secondsRemaining !in 1L..MAX_TTL_SECONDS) return false
        val expiry = status.entitlementExpiresAt ?: return false
        val remaining = expiry - nowSeconds
        return remaining in 1L..(MAX_TTL_SECONDS + CLOCK_SKEW_SECONDS)
    }
}
