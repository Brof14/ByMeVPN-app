package com.example.bymevpn

import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.subscription.DeviceBootstrapPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceBootstrapPolicyTest {
    private val now = 1_800_000_000L

    @Test
    fun acceptsOnlyShortSignedBootstrapForSupportedSubscriptionAndDeviceLimits() {
        assertTrue(DeviceBootstrapPolicy.isValid(bootstrap(), now))
        assertTrue(DeviceBootstrapPolicy.isValid(bootstrap(status = "active", devices = 5), now))
        assertTrue(DeviceBootstrapPolicy.isValid(bootstrap(status = "grace", devices = 10), now))
        assertFalse(DeviceBootstrapPolicy.isValid(bootstrap(expiry = now), now))
        assertFalse(DeviceBootstrapPolicy.isValid(bootstrap(expiry = now + 661), now))
        assertFalse(DeviceBootstrapPolicy.isValid(bootstrap(seconds = 601), now))
        assertFalse(DeviceBootstrapPolicy.isValid(bootstrap(devices = 1), now))
        assertFalse(DeviceBootstrapPolicy.isValid(bootstrap(status = "none"), now))
        assertFalse(DeviceBootstrapPolicy.isValid(bootstrap(expiry = null), now))
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
