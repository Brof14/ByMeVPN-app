package com.example.bymevpn

import com.example.bymevpn.data.api.AccountActionErrorMessages
import com.example.bymevpn.data.api.ApiError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountActionErrorMessagesTest {
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
