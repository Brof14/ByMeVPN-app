package com.example.bymevpn.data.subscription

import com.example.bymevpn.data.api.SubscriptionStatus

/** Keeps explicitly imported third-party profiles isolated from authenticated ByMeVPN accounts. */
internal object ExternalProfileAccessPolicy {
    fun canConnect(
        externalMode: Boolean,
        hasAccountToken: Boolean,
        profileId: String?,
        installId: String,
        hasImportedConfigs: Boolean,
        status: SubscriptionStatus,
        nowMillis: Long
    ): Boolean = externalMode &&
        !hasAccountToken &&
        !profileId.isNullOrBlank() && profileId == installId &&
        hasImportedConfigs &&
        status.isUsable &&
        !status.knownExpiryReached(nowMillis)
}
