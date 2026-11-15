package com.example.bymevpn

import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.subscription.ExternalProfileAccessPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalProfileAccessPolicyTest {
    private val active = SubscriptionStatus(status = "active", lastFetchedAt = 1_000L)

    @Test
    fun permitsOnlyExplicitLocalProfileWithImportedConfigs() {
        assertTrue(ExternalProfileAccessPolicy.canConnect(true, false, "install-1", "install-1", true, active, 2_000L))
        assertFalse(ExternalProfileAccessPolicy.canConnect(false, false, "install-1", "install-1", true, active, 2_000L))
        assertFalse(ExternalProfileAccessPolicy.canConnect(true, true, "install-1", "install-1", true, active, 2_000L))
        assertFalse(ExternalProfileAccessPolicy.canConnect(true, false, "another-install", "install-1", true, active, 2_000L))
        assertFalse(ExternalProfileAccessPolicy.canConnect(true, false, "install-1", "install-1", false, active, 2_000L))
    }

    @Test
    fun expiredAndInactiveExternalProfilesCannotConnect() {
        val expiring = active.copy(secondsRemaining = 10, lastFetchedAt = 1_000L)
        assertTrue(ExternalProfileAccessPolicy.canConnect(true, false, "install-1", "install-1", true, expiring, 5_000L))
        assertFalse(ExternalProfileAccessPolicy.canConnect(true, false, "install-1", "install-1", true, expiring, 11_000L))
        assertFalse(ExternalProfileAccessPolicy.canConnect(true, false, "install-1", "install-1", true, active.copy(status = "expired"), 2_000L))
    }
}
