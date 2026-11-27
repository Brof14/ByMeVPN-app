package com.example.bymevpn.data.storage

/** Prevents account-owned network results from being committed after a session switch. */
internal object AccountSessionCommitPolicy {
    fun canCommit(
        expectedSessionId: String,
        currentSessionId: String,
        hasAccessToken: Boolean,
        externalProfileMode: Boolean
    ): Boolean =
        expectedSessionId.isNotBlank() &&
            expectedSessionId == currentSessionId &&
            hasAccessToken &&
            !externalProfileMode
}
