package com.example.bymevpn

import com.example.bymevpn.vpn.isServerCacheFresh
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerCachePolicyTest {
    @Test
    fun expiresAtSixHoursAndRejectsInvalidClockValues() {
        val saved = 1_000_000L
        assertTrue(isServerCacheFresh(saved, saved))
        assertTrue(isServerCacheFresh(saved, saved + 6 * 60 * 60 * 1000L - 1))
        assertFalse(isServerCacheFresh(saved, saved + 6 * 60 * 60 * 1000L))
        assertFalse(isServerCacheFresh(saved, saved - 1))
        assertFalse(isServerCacheFresh(0, saved))
    }
}
