package com.example.bymevpn.data.api

/** Whether a refresh failure proves the session is revoked or merely unreachable. */
internal enum class RefreshSessionResult { REFRESHED, REJECTED, TEMPORARY_FAILURE }

internal object RefreshSessionPolicy {
    // Only explicit invalid refresh-token responses justify a destructive logout.
    // 403/429/5xx, network failures and malformed responses are not proof of revocation.
    fun fromHttpStatus(httpStatus: Int): RefreshSessionResult =
        if (httpStatus == 400 || httpStatus == 401) RefreshSessionResult.REJECTED
        else RefreshSessionResult.TEMPORARY_FAILURE

    /** A rotated bearer may be reused only for the same verified local account. */
    fun sameAccountCanRetry(
        requestedOwnerId: String?,
        currentOwnerId: String?,
        currentAccessToken: String?
    ): Boolean =
        !requestedOwnerId.isNullOrBlank() &&
        requestedOwnerId == currentOwnerId &&
        !currentAccessToken.isNullOrBlank()
}
