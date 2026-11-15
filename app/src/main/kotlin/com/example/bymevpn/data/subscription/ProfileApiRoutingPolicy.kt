package com.example.bymevpn.data.subscription

/** A standalone imported profile must never call account-protected ByMeVPN endpoints. */
internal object ProfileApiRoutingPolicy {
    fun useAccountApi(externalProfileMode: Boolean): Boolean = !externalProfileMode
}

/** A failed account refresh may clear a session without deleting a standalone profile. */
internal object ExpiredSessionCleanupPolicy {
    fun preserveImportedProfile(externalProfileMode: Boolean): Boolean = externalProfileMode
}
