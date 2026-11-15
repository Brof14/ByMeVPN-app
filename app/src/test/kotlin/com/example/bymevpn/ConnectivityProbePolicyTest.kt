package com.example.bymevpn

import com.example.bymevpn.vpn.ConnectivityProbePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityProbePolicyTest {
    @Test
    fun aSingleUnavailableProbeDoesNotMarkTheTunnelUnavailable() {
        val attempted = mutableListOf<String>()

        val reachable = ConnectivityProbePolicy.anyTargetReachable { host ->
            attempted += host
            host == "www.google.com"
        }

        assertTrue(reachable)
        assertEquals(listOf("www.cloudflare.com", "www.google.com"), attempted)
    }

    @Test
    fun allIndependentTargetsMustFailBeforeRoundIsUnavailable() {
        assertFalse(ConnectivityProbePolicy.anyTargetReachable { false })
    }

    @Test
    fun requiresThreeConsecutiveFailedRoundsBeforeDisconnecting() {
        assertFalse(ConnectivityProbePolicy.shouldDisconnect(1))
        assertFalse(ConnectivityProbePolicy.shouldDisconnect(2))
        assertTrue(ConnectivityProbePolicy.shouldDisconnect(3))
    }
}
