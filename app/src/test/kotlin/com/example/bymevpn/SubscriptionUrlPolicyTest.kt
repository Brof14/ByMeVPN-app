package com.example.bymevpn

import com.example.bymevpn.data.subscription.SubscriptionUrlPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionUrlPolicyTest {

    private val apiBase = "https://bymevpn.duckdns.org/api/v1"

    @Test
    fun keepsPublicHttpsUrlUntouched() {
        val raw = "https://bymevpn.duckdns.org:2096/sub/abc123?token=x"
        val resolved = SubscriptionUrlPolicy.resolve(raw, apiBase)
        assertFalse(resolved.rewritten)
        assertEquals(raw, resolved.url.toString())
    }

    @Test
    fun rewritesLoopbackUrlToThePublicBackendHost() {
        val resolved = SubscriptionUrlPolicy.resolve(
            "http://127.0.0.1:2096/sub/abc123?token=x",
            apiBase
        )
        assertTrue(resolved.rewritten)
        assertEquals(
            "https://bymevpn.duckdns.org:2096/sub/abc123?token=x",
            resolved.url.toString()
        )
    }

    @Test
    fun loopbackWithoutPortFallsBackToThePanelPort() {
        val resolved = SubscriptionUrlPolicy.resolve("https://localhost/sub/abc", apiBase)
        assertTrue(resolved.rewritten)
        assertEquals("https://bymevpn.duckdns.org:2096/sub/abc", resolved.url.toString())
    }

    @Test
    fun upgradesPlainHttpToHttps() {
        val resolved = SubscriptionUrlPolicy.resolve("http://example.com/sub/abc", apiBase)
        assertTrue(resolved.rewritten)
        assertEquals("https://example.com/sub/abc", resolved.url.toString())
    }
}
