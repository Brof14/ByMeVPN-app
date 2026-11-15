package com.example.bymevpn.data.subscription

import com.example.bymevpn.data.api.SubscriptionStatus

/** An unavailable status is not evidence that an account has no subscription. */
internal object SubscriptionPresentationPolicy {
    fun isUnknown(status: SubscriptionStatus?): Boolean =
        status == null || status.isUndetermined

    /** An active plan with an invalid/missing device entitlement needs an access check, not a trial offer. */
    fun requiresAccessConfirmation(status: SubscriptionStatus?, canConnect: Boolean): Boolean =
        status?.isUsable == true && !canConnect
}
