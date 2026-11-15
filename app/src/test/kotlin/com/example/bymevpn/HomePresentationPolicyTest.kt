package com.example.bymevpn

import com.example.bymevpn.screens.HomePresentationPolicy
import com.example.bymevpn.vpn.VpnConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePresentationPolicyTest {
    @Test fun liveVpnCannotBeHiddenByTemporarilyUnknownBillingStatus() {
        for (state in listOf(VpnConnectionState.CONNECTED, VpnConnectionState.CONNECTING)) {
            val expected = if (state == VpnConnectionState.CONNECTED)
                HomePresentationPolicy.Screen.CONNECTED
            else HomePresentationPolicy.Screen.CONNECTING
            assertEquals(expected, HomePresentationPolicy.select(state, false, false, true, true))
        }
    }

    @Test fun noPlanAndErrorsStillHaveDedicatedRecoveryScreens() {
        assertEquals(HomePresentationPolicy.Screen.ACCESS_REQUIRED,
            HomePresentationPolicy.select(VpnConnectionState.DISCONNECTED, false, false, false, false))
        assertEquals(HomePresentationPolicy.Screen.ERROR,
            HomePresentationPolicy.select(VpnConnectionState.ERROR, true, false, false, false))
        assertEquals(HomePresentationPolicy.Screen.DISCONNECTED,
            HomePresentationPolicy.select(VpnConnectionState.DISCONNECTED, true, false, false, false))
    }
}
