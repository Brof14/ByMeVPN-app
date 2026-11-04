package com.example.bymevpn

import com.example.bymevpn.data.subscription.SubscriptionStatusSourcePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionStatusSourcePolicyTest {
    @Test
    fun signedEntitlementBuildAlwaysUsesAuthenticatedBackendStatus() {
        assertFalse(
            SubscriptionStatusSourcePolicy.shouldUseSubscriptionFeed(
                forceBackend = false,
                subscriptionUrl = "malformed legacy URL",
                signedEntitlementsEnabled = true
            )
        )
    }

    @Test
    fun explicitRefreshAndMissingUrlUseBackendStatus() {
        assertFalse(
            SubscriptionStatusSourcePolicy.shouldUseSubscriptionFeed(
                forceBackend = true,
                subscriptionUrl = "https://example.test/sub/token",
                signedEntitlementsEnabled = false
            )
        )
        assertFalse(
            SubscriptionStatusSourcePolicy.shouldUseSubscriptionFeed(
                forceBackend = false,
                subscriptionUrl = null,
                signedEntitlementsEnabled = false
            )
        )
    }

    @Test
    fun legacyBuildCanStillUseValidSubscriptionFeed() {
        assertTrue(
            SubscriptionStatusSourcePolicy.shouldUseSubscriptionFeed(
                forceBackend = false,
                subscriptionUrl = "https://example.test/sub/token",
                signedEntitlementsEnabled = false
            )
        )
    }
}
