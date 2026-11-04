package com.example.bymevpn.data.subscription

/** Keeps unsigned panel feeds from replacing authenticated account entitlements. */
internal object SubscriptionStatusSourcePolicy {
    fun shouldUseSubscriptionFeed(
        forceBackend: Boolean,
        subscriptionUrl: String?,
        signedEntitlementsEnabled: Boolean
    ): Boolean =
        !forceBackend && !subscriptionUrl.isNullOrBlank() && !signedEntitlementsEnabled
}
