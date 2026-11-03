package com.example.bymevpn

import com.example.bymevpn.data.api.ApiError
import com.example.bymevpn.data.auth.TelegramAuthPollPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramAuthPollPolicyTest {
    @Test
    fun retriesOnlyTemporaryFailuresAndStopsAfterBoundedAttempts() {
        assertTrue(TelegramAuthPollPolicy.shouldRetry(ApiError("NETWORK_ERROR", "hidden"), 0))
        assertTrue(TelegramAuthPollPolicy.shouldRetry(ApiError("HTTP_503", "hidden", 503), 1))
        assertFalse(TelegramAuthPollPolicy.shouldRetry(ApiError("SESSION_EXPIRED", "hidden", 401), 0))
        assertFalse(TelegramAuthPollPolicy.shouldRetry(ApiError("HTTP_503", "hidden", 503), 2))
        assertFalse(TelegramAuthPollPolicy.shouldRetry(IllegalStateException("hidden"), 0))
    }
}
