package com.example.bymevpn

import com.example.bymevpn.data.auth.isByMeTelegramBotLink
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramBotLinkTest {
    @Test
    fun acceptsOnlyExpectedBotHttpsLink() {
        assertTrue(isByMeTelegramBotLink("https://t.me/ByMeVPNbot?start=auth_test-1"))
        assertFalse(isByMeTelegramBotLink("http://t.me/ByMeVPNbot?start=auth_test"))
        assertFalse(isByMeTelegramBotLink("https://t.me.example.org/ByMeVPNbot?start=auth_test"))
        assertFalse(isByMeTelegramBotLink("https://t.me/OtherBot?start=auth_test"))
        assertFalse(isByMeTelegramBotLink("https://t.me/ByMeVPNbot?start=auth_test&other=1"))
    }
}
