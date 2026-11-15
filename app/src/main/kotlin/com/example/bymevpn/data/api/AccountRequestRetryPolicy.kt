package com.example.bymevpn.data.api

/** A 401 may be retried only within the account session which sent the request. */
internal object AccountRequestRetryPolicy {
    fun sameSession(requestSessionId: String?, activeSessionId: String?): Boolean =
        !requestSessionId.isNullOrBlank() && requestSessionId == activeSessionId
}
