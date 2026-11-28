package com.example.bymevpn

import com.example.bymevpn.data.subscription.SubscriptionRefreshError
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionRefreshErrorTest {
    @Test
    fun networkFailureExplainsThatBackendCouldNotBeReached() {
        val message = SubscriptionRefreshError(code = "NETWORK_ERROR").userMessage(isRu = true)
        assertTrue(message.contains("Нет соединения"))
    }

    @Test
    fun unauthorizedStatusAsksForAuthenticationAgain() {
        val message = SubscriptionRefreshError(code = "UNAUTHORIZED", httpStatusCode = 401).userMessage(isRu = true)
        assertTrue(message.contains("войдите в аккаунт заново", ignoreCase = true))
    }

    @Test
    fun backendUnavailableShowsRetryableServerFailure() {
        val message = SubscriptionRefreshError(code = "API_ERROR", httpStatusCode = 503).userMessage(isRu = true)
        assertTrue(message.contains("временно не может подтвердить доступ"))
    }

    @Test
    fun correlationIdIsPreservedForSupportDiagnostics() {
        val error = SubscriptionRefreshError(
            code = "API_ERROR",
            httpStatusCode = 503,
            correlationId = "status-check-1234"
        )
        assertTrue(error.correlationId == "status-check-1234")
    }
}
