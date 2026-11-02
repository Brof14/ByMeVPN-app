package com.example.bymevpn

import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.subscription.TrialBootstrapPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialBootstrapPolicyTest {
    private val now = 1_800_000_000L

    @Test
    fun acceptsOnlyShortTwoDeviceTrialBootstrap() {
        assertTrue(TrialBootstrapPolicy.isValid(bootstrap(), now))
        assertFalse(TrialBootstrapPolicy.isValid(bootstrap(expiry = now), now))
        assertFalse(TrialBootstrapPolicy.isValid(bootstrap(expiry = now + 661), now))
        assertFalse(TrialBootstrapPolicy.isValid(bootstrap(seconds = 601), now))
        assertFalse(TrialBootstrapPolicy.isValid(bootstrap(devices = 1), now))
        assertFalse(TrialBootstrapPolicy.isValid(bootstrap(status = "active"), now))
        assertFalse(TrialBootstrapPolicy.isValid(bootstrap(expiry = null), now))
    }

    private fun bootstrap(
        status: String = "trial",
        devices: Int = 2,
        seconds: Long = 600,
        expiry: Long? = now + 600
    ) = SubscriptionStatus(
        status = status,
        maxDevices = devices,
        secondsRemaining = seconds,
        entitlementBootstrap = true,
        entitlementExpiresAt = expiry
    )
}
