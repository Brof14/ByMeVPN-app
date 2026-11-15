package com.example.bymevpn

import com.example.bymevpn.vpn.VpnServiceRestorePolicy
import com.example.bymevpn.vpn.VpnConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnServiceRestorePolicyTest {
    @Test
    fun restoreTimeoutIsFiniteAndAllowsSlowBackendRefresh() {
        assertTrue(VpnServiceRestorePolicy.RESTORE_TIMEOUT_MS in 60_000L..180_000L)
    }

    @Test
    fun doesNotStartVpnWhenThereWasNoPreviousSession() {
        assertFalse(VpnServiceRestorePolicy.shouldRestore(previouslyActive = false, systemAlwaysOn = false))
    }

    @Test
    fun restoresAnPreviouslyActiveSessionOrSystemAlwaysOnVpn() {
        assertTrue(VpnServiceRestorePolicy.shouldRestore(previouslyActive = true, systemAlwaysOn = false))
        assertTrue(VpnServiceRestorePolicy.shouldRestore(previouslyActive = false, systemAlwaysOn = true))
    }

    @Test
    fun restoreOutcomeAlwaysHasOneTerminalResultAndWaitsForTransitions() {
        assertEquals(VpnServiceRestorePolicy.Outcome.RESTORED, VpnServiceRestorePolicy.outcomeFor(VpnConnectionState.CONNECTED))
        assertEquals(VpnServiceRestorePolicy.Outcome.FAILED, VpnServiceRestorePolicy.outcomeFor(VpnConnectionState.ERROR))
        assertEquals(VpnServiceRestorePolicy.Outcome.CANCELLED, VpnServiceRestorePolicy.outcomeFor(VpnConnectionState.DISCONNECTED))
        assertNull(VpnServiceRestorePolicy.outcomeFor(VpnConnectionState.CONNECTING))
        assertNull(VpnServiceRestorePolicy.outcomeFor(VpnConnectionState.DISCONNECTING))
    }
}
