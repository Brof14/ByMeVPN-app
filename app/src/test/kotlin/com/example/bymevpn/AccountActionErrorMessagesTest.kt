package com.example.bymevpn

import com.example.bymevpn.data.api.AccountActionErrorMessages
import com.example.bymevpn.data.api.ApiError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountActionErrorMessagesTest {
    @Test
    fun apiErrorPreservesOnlySafeCorrelationIdsFromBackendBody() {
        val valid = ApiError.fromJson(
            """{"detail":"Sign-in failed","correlation_id":"abc-1234"}"""
        )
        assertEquals("abc-1234", valid.correlationId)

        val unsafe = ApiError.fromJson(
            """{"detail":"Sign-in failed","correlation_id":"https://private.example/token"}"""
        )
        assertEquals(null, unsafe.correlationId)
    }

    @Test
    fun googleFailureExplainsConnectionAndBackendAvailabilitySeparately() {
        assertTrue(
            AccountActionErrorMessages.googleSignIn(ApiError("NETWORK_ERROR", "private detail"), true)
                .contains("интернет")
        )
        assertTrue(
            AccountActionErrorMessages.googleSignIn(ApiError("HTTP_503", "private detail", 503), true)
                .contains("временно недоступен")
        )
        assertEquals(
            "",
            AccountActionErrorMessages.googleSignIn(ApiError("USER_CANCELLED", "cancelled"), true)
        )

        val denied = AccountActionErrorMessages.googleSignIn(
            ApiError("API_ERROR", "private detail", 401, correlationId = "a1b2c3d4e5f60708"), true
        )
        assertTrue(denied.contains("вход не завершился"))
        assertTrue(denied.contains("a1b2c3d4e5f60708"))
        assertTrue(!denied.contains("private detail"))
    }

    @Test
    fun telegramAndTrialFailuresGiveUsefulLocalizedGuidanceAndSupportCode() {
        val telegram = AccountActionErrorMessages.telegramSignInStart(
            ApiError("HTTP_503", "private detail", 503, correlationId = "support-1234"), true
        )
        assertTrue(telegram.contains("временно недоступен"))
        assertTrue(telegram.contains("support-1234"))
        assertTrue(!telegram.contains("private detail"))

        val trial = AccountActionErrorMessages.activateTrial(
            ApiError("API_ERROR", "private detail", 422, correlationId = "trial-1234"), true
        )
        assertTrue(trial.contains("подтвердить это устройство"))
        assertTrue(trial.contains("trial-1234"))
        assertTrue(!trial.contains("private detail"))
    }

    @Test
    fun telegramConfirmationFailureExplainsThatBotConfirmationWasNotEnoughAndKeepsSupportCode() {
        val failure = AccountActionErrorMessages.telegramSignInCheck(
            ApiError("HTTP_500", "private backend detail", 500, correlationId = "tg-5678"), true
        )
        assertTrue(failure.contains("Бот подтвердил вход"))
        assertTrue(failure.contains("tg-5678"))
        assertTrue(!failure.contains("private backend detail"))
    }

    @Test
    fun deviceDeleteFailureExplainsSessionAndRetryWithoutShowingServerDetails() {
        val expired = AccountActionErrorMessages.deleteDevice(
            ApiError("UNAUTHORIZED", "internal detail", 401), true
        )
        assertTrue(expired.contains("Сеанс завершился"))
        assertTrue(!expired.contains("internal detail"))

        val unavailable = AccountActionErrorMessages.deleteDevice(
            ApiError("HTTP_503", "internal detail", 503), true
        )
        assertTrue(unavailable.contains("Сервер не смог удалить"))
        assertTrue(!unavailable.contains("internal detail"))
    }
}
