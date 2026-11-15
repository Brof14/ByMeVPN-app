package com.example.bymevpn.vpn

/** A recovered physical link may restart a failed tunnel only when the user opted in. */
internal object PhysicalNetworkRecoveryPolicy {
    fun shouldRetry(
        reconnectEnabled: Boolean,
        recoveryPending: Boolean,
        state: VpnConnectionState
    ): Boolean = reconnectEnabled && recoveryPending &&
        state != VpnConnectionState.CONNECTED &&
        state != VpnConnectionState.CONNECTING &&
        state != VpnConnectionState.DISCONNECTING
}
