package com.example.bymevpn

import com.example.bymevpn.vpn.PhysicalNetworkRecoveryPolicy
import com.example.bymevpn.vpn.VpnConnectionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicalNetworkRecoveryPolicyTest {
    @Test
    fun retriesOnlyAfterOptedInRecoveryWhenTunnelIsNotAlreadyStarting() {
        assertTrue(
            PhysicalNetworkRecoveryPolicy.shouldRetry(
                reconnectEnabled = true,
                recoveryPending = true,
                state = VpnConnectionState.ERROR
            )
        )
        assertFalse(
            PhysicalNetworkRecoveryPolicy.shouldRetry(
                reconnectEnabled = false,
                recoveryPending = true,
                state = VpnConnectionState.ERROR
            )
        )
        assertFalse(
            PhysicalNetworkRecoveryPolicy.shouldRetry(
                reconnectEnabled = true,
                recoveryPending = false,
                state = VpnConnectionState.ERROR
            )
        )
        assertFalse(
            PhysicalNetworkRecoveryPolicy.shouldRetry(
                reconnectEnabled = true,
                recoveryPending = true,
                state = VpnConnectionState.CONNECTING
            )
        )
    }
}
