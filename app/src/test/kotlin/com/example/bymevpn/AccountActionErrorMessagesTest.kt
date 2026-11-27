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

    @Test
    fun supportCodeLabelFollowsSelectedLanguage() {
        val enTrial = AccountActionErrorMessages.activateTrial(
            ApiError("HTTP_500", "internal", 500, correlationId = "a17fe10f9e8e6211"), false
        )
        assertTrue(enTrial.contains("(code: a17fe10f9e8e6211)"))
        assertTrue(!enTrial.contains("код"))

        val ruTrial = AccountActionErrorMessages.activateTrial(
            ApiError("HTTP_500", "internal", 500, correlationId = "a17fe10f9e8e6211"), true
        )
        assertTrue(ruTrial.contains("(код: a17fe10f9e8e6211)"))
    }

    @Test
    fun googleCredentialExceptionsGiveClearLocalizedGuidance() {
        val noAccountRu = AccountActionErrorMessages.googleSignIn(
            ApiError("NO_GOOGLE_ACCOUNT", "No Google account"), true
        )
        assertTrue(noAccountRu.contains("аккаунт Google"))

        val noAccountEn = AccountActionErrorMessages.googleSignIn(
            ApiError("NO_GOOGLE_ACCOUNT", "No Google account"), false
        )
        assertTrue(noAccountEn.contains("No Google account"))

        val unavailableRu = AccountActionErrorMessages.googleSignIn(
            ApiError("GOOGLE_CREDENTIAL_UNAVAILABLE", "Unavailable"), true
        )
        assertTrue(unavailableRu.contains("недоступен"))

        val unavailableEn = AccountActionErrorMessages.googleSignIn(
            ApiError("GOOGLE_CREDENTIAL_UNAVAILABLE", "Unavailable"), false
        )
        assertTrue(unavailableEn.contains("unavailable"))

        val configErrRu = AccountActionErrorMessages.googleSignIn(
            ApiError("GOOGLE_AUTH_CONFIG_ERROR", "DEV_ERROR"), true
        )
        assertTrue(configErrRu.contains("конфигурации"))

        val configErrEn = AccountActionErrorMessages.googleSignIn(
            ApiError("GOOGLE_AUTH_CONFIG_ERROR", "DEV_ERROR"), false
        )
        assertTrue(configErrEn.contains("configuration error"))

        val interruptedRu = AccountActionErrorMessages.googleSignIn(
            ApiError("GOOGLE_AUTH_INTERRUPTED", "Interrupted"), true
        )
        assertTrue(interruptedRu.contains("прерван"))

        val interruptedEn = AccountActionErrorMessages.googleSignIn(
            ApiError("GOOGLE_AUTH_INTERRUPTED", "Interrupted"), false
        )
        assertTrue(interruptedEn.contains("interrupted"))
    }
}
