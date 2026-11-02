package com.example.bymevpn

import com.example.bymevpn.data.auth.isByMeTelegramBotLink
import com.example.bymevpn.data.api.TelegramAuthRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramBotLinkTest {

    @Test
    fun telegramSessionStateIsSentInJsonBodyNotQueryString() {
        val token = "auth_one-time-state"
        val body = TelegramAuthRequest.sessionBody(token)

        assertEquals("/auth/telegram/check-session", TelegramAuthRequest.CHECK_SESSION_PATH)
        assertFalse(TelegramAuthRequest.CHECK_SESSION_PATH.contains("?"))
        assertEquals(token, body.getString("token"))
        assertEquals(1, body.length())
        assertTrue(TelegramAuthRequest.shouldFallbackToLegacyGet(405))
        assertFalse(TelegramAuthRequest.shouldFallbackToLegacyGet(404))
        assertTrue(TelegramAuthRequest.legacyGetPath("auth state").endsWith("?token=auth+state"))
    }
    @Test
    fun acceptsOnlyExpectedBotHttpsLink() {
        assertTrue(isByMeTelegramBotLink("https://t.me/ByMeVPNbot?start=auth_test-1"))
        assertFalse(isByMeTelegramBotLink("http://t.me/ByMeVPNbot?start=auth_test"))
        assertFalse(isByMeTelegramBotLink("https://t.me.example.org/ByMeVPNbot?start=auth_test"))
        assertFalse(isByMeTelegramBotLink("https://t.me/OtherBot?start=auth_test"))
        assertFalse(isByMeTelegramBotLink("https://t.me/ByMeVPNbot?start=auth_test&other=1"))
    }
}
