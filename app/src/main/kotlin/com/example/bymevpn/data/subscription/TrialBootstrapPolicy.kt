package com.example.bymevpn.data.subscription

import com.example.bymevpn.data.api.SubscriptionStatus

/** Local bounds for the server-signed first-device bootstrap window. */
internal object TrialBootstrapPolicy {
    private const val MAX_TTL_SECONDS = 10 * 60L
    private const val CLOCK_SKEW_SECONDS = 60L

    fun isValid(status: SubscriptionStatus, nowSeconds: Long): Boolean {
        if (!status.entitlementBootstrap || status.status != "trial" || status.maxDevices != 2) return false
        if (status.secondsRemaining !in 1L..MAX_TTL_SECONDS) return false
        val expiry = status.entitlementExpiresAt ?: return false
        val remaining = expiry - nowSeconds
        return remaining in 1L..(MAX_TTL_SECONDS + CLOCK_SKEW_SECONDS)
    }
}
