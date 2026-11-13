package com.example.bymevpn.data

import com.example.bymevpn.data.api.ApiError

/** Decides when a rejected API session should clear the in-memory account state. */
internal object AccountSessionPolicy {
    fun isExpired(error: Exception): Boolean {
        val apiError = error as? ApiError ?: return false
        return apiError.code == "UNAUTHORIZED" || apiError.httpStatusCode == 401
    }
}
