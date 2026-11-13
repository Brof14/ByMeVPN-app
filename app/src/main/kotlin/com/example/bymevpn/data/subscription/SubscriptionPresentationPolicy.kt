package com.example.bymevpn.data.subscription

import com.example.bymevpn.data.api.SubscriptionStatus

/** An unavailable status is not evidence that an account has no subscription. */
internal object SubscriptionPresentationPolicy {
    fun isUnknown(status: SubscriptionStatus?): Boolean =
        status == null || status.isUndetermined
}
}
