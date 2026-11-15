package com.example.bymevpn

import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.subscription.SubscriptionPresentationPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionPresentationPolicyTest {
    @Test
    fun unavailableStatusIsNotPresentedAsAnInactiveSubscription() {
        assertTrue(SubscriptionPresentationPolicy.isUnknown(null))
        assertTrue(SubscriptionPresentationPolicy.isUnknown(SubscriptionStatus(status = "unknown")))
        assertFalse(SubscriptionPresentationPolicy.isUnknown(SubscriptionStatus(status = "none")))
        assertFalse(SubscriptionPresentationPolicy.isUnknown(SubscriptionStatus(status = "active")))
    }

    @Test
    fun activePlanWithoutVerifiedDeviceAccessRequiresRefreshInsteadOfTrial() {
        assertTrue(
            SubscriptionPresentationPolicy.requiresAccessConfirmation(
                SubscriptionStatus(status = "active", secondsRemaining = 3600),
                canConnect = false
            )
        )
        assertFalse(
            SubscriptionPresentationPolicy.requiresAccessConfirmation(
                SubscriptionStatus(status = "active", secondsRemaining = 3600),
                canConnect = true
            )
        )
        assertFalse(
            SubscriptionPresentationPolicy.requiresAccessConfirmation(
                SubscriptionStatus(status = "none"),
                canConnect = false
            )
        )
    }
}
