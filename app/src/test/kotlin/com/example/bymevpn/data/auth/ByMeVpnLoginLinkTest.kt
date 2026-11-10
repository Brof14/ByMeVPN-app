package com.example.bymevpn.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ByMeVpnLoginLinkTest {
    @Test
    fun recognizesOnlyExplicitTelegramLoginHandoff() {
        assertTrue(isTelegramLoginHandoff("bymevpn://login?auth=telegram"))
        assertFalse(isTelegramLoginHandoff("bymevpn://login?auth=email"))
        assertFalse(isTelegramLoginHandoff("bymevpn://other?auth=telegram"))
        assertFalse(isTelegramLoginHandoff("https://bymevpn.duckdns.org/login?auth=telegram"))
        assertFalse(isTelegramLoginHandoff("bymevpn://login?auth=telegram&sub=https%3A%2F%2Fexample.org%2Fsecret"))
        assertFalse(isTelegramLoginHandoff("bymevpn://login?auth=telegram&auth=telegram"))
    }
}
