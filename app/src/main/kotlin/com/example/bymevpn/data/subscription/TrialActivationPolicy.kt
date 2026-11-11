package com.example.bymevpn.data.subscription

import com.example.bymevpn.data.api.SubscriptionStatus

/** Server-reported eligibility controls whether the home screen offers one-tap trial activation. */
internal object TrialActivationPolicy {
    fun shouldOfferTrial(status: SubscriptionStatus?): Boolean =
        status != null && !status.isUsable && status.trialAvailable

    fun isConfirmedActiveTrial(status: SubscriptionStatus?, nowMillis: Long): Boolean =
        status?.status == "trial" && status.isUsable && status.secondsRemaining > 0 &&
            !status.knownExpiryReached(nowMillis)
}
