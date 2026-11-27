package com.example.bymevpn

import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.vpn.PingSelectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PingSelectionPolicyTest {

    private fun node(
        code: String,
        pingMs: Int = -1,
        loadPercent: Int = 0,
        connectionCheck: Int = 0,
        protocol: String = "VLESS Reality"
    ) = ServerNode(
        nodeCode = code,
        country = code,
        countryCode = "DE",
        city = code,
        flag = "",
        pingMs = pingMs,
        loadPercent = loadPercent,
        isPremium = false,
        protocol = protocol,
        displayName = code,
        connectionCheck = connectionCheck
    )

    @Test
    fun cacheTtlDistinguishesSuccessFromFailure() {
        assertTrue(PingSelectionPolicy.shouldUseCachedPing(65, 5 * 60 * 1000L))
        assertFalse(PingSelectionPolicy.shouldUseCachedPing(65, 11 * 60 * 1000L))
        assertTrue(PingSelectionPolicy.shouldUseCachedPing(-1, 15 * 1000L))
        assertFalse(PingSelectionPolicy.shouldUseCachedPing(-2, 45 * 1000L))
        assertFalse(PingSelectionPolicy.shouldUseCachedPing(50, -1L))
    }

    @Test
    fun aggregateLatencyIgnoresOutlierSlowSample() {
        assertEquals(-1, PingSelectionPolicy.aggregateLatencySamples(listOf(-1, 0)))
        assertEquals(80, PingSelectionPolicy.aggregateLatencySamples(listOf(80, -1)))
        assertEquals(85, PingSelectionPolicy.aggregateLatencySamples(listOf(80, 90)))
        assertEquals(65, PingSelectionPolicy.aggregateLatencySamples(listOf(65, 420)))
    }

    @Test
    fun pickBestServerSkipsPreviouslyFailedWhenHealthyMeasuredExists() {
        val failedFast = node("de-fail", pingMs = 20, connectionCheck = -1)
        val healthy = node("fi-ok", pingMs = 75, connectionCheck = 0)
        val slower = node("us-slow", pingMs = 190, connectionCheck = 1)

        val picked = PingSelectionPolicy.pickBestServer(listOf(failedFast, slower, healthy))
        assertEquals("fi-ok", picked?.nodeCode)
    }

    @Test
    fun pickBestServerBreaksLatencyTiesByLoadAndProtocol() {
        val loaded = node("n1", pingMs = 50, loadPercent = 80, protocol = "VLESS Reality")
        val lightlyLoaded = node("n2", pingMs = 50, loadPercent = 20, protocol = "VLESS Reality")

        val picked = PingSelectionPolicy.pickBestServer(listOf(loaded, lightlyLoaded))
        assertEquals("n2", picked?.nodeCode)
    }
}
