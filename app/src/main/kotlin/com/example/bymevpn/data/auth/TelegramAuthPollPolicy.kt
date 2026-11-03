package com.example.bymevpn.data.auth

import com.example.bymevpn.data.api.ApiError

/** Bound retries for temporary transport/server errors while polling a confirmed Telegram session. */
object TelegramAuthPollPolicy {
    const val MAX_TRANSIENT_RETRIES = 2

    fun shouldRetry(error: Throwable, retriesAlreadyUsed: Int): Boolean {
        if (retriesAlreadyUsed >= MAX_TRANSIENT_RETRIES) return false
        val apiError = error as? ApiError ?: return false
        return apiError.code == "NETWORK_ERROR" || apiError.httpStatusCode?.let { it in 500..599 } == true
    }
}
