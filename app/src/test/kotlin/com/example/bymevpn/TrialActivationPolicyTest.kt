package com.example.bymevpn

import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.subscription.TrialActivationPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialActivationPolicyTest {
    @Test
    fun activatesOnlyWhenServerReportsTrialAvailableAndNoUsableAccess() {
        assertTrue(
            TrialActivationPolicy.shouldAutoActivate(
                SubscriptionStatus(status = "none", trialAvailable = true)
            )
        )
        assertFalse(
            TrialActivationPolicy.shouldAutoActivate(
                SubscriptionStatus(status = "active", trialAvailable = true)
            )
        )
        assertFalse(
            TrialActivationPolicy.shouldAutoActivate(
                SubscriptionStatus(status = "trial", trialAvailable = true)
            )
        )
        assertFalse(
            TrialActivationPolicy.shouldAutoActivate(
                SubscriptionStatus(status = "none", trialAvailable = false)
            )
        )
        assertFalse(TrialActivationPolicy.shouldAutoActivate(null))
    }

    @Test
    fun recognizesOnlyFreshUsableTrialForDuplicateActivationRecovery() {
        val now = 1_800_000_000_000L
        assertTrue(
            TrialActivationPolicy.isConfirmedActiveTrial(
                SubscriptionStatus(status = "trial", secondsRemaining = 120, lastFetchedAt = now), now
            )
        )
        assertFalse(
            TrialActivationPolicy.isConfirmedActiveTrial(
                SubscriptionStatus(status = "trial", secondsRemaining = 120, lastFetchedAt = now - 121_000), now
            )
        )
        assertFalse(
            TrialActivationPolicy.isConfirmedActiveTrial(
                SubscriptionStatus(status = "trial", secondsRemaining = 0, lastFetchedAt = now), now
            )
        )
        assertFalse(
            TrialActivationPolicy.isConfirmedActiveTrial(
                SubscriptionStatus(status = "active", secondsRemaining = 120, lastFetchedAt = now), now
            )
        )
    }
}
