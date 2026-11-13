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
}
