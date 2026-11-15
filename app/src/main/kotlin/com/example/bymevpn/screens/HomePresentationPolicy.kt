package com.example.bymevpn.screens

import com.example.bymevpn.vpn.VpnConnectionState

/**
 * Present an already-active or connecting tunnel before subscription recovery
 * prompts. A temporary billing API outage must never hide the disconnect action.
 */
internal object HomePresentationPolicy {
    enum class Screen { CONNECTED, CONNECTING, ERROR, ACCESS_REQUIRED, DISCONNECTED }

    fun select(
        vpnState: VpnConnectionState,
        hasPlan: Boolean,
        isActivatingTrial: Boolean,
        subscriptionUnknown: Boolean,
        accessConfirmationRequired: Boolean
    ): Screen = when {
        vpnState == VpnConnectionState.CONNECTED -> Screen.CONNECTED
        vpnState == VpnConnectionState.CONNECTING -> Screen.CONNECTING
        !hasPlan || isActivatingTrial || subscriptionUnknown || accessConfirmationRequired -> Screen.ACCESS_REQUIRED
        vpnState == VpnConnectionState.ERROR -> Screen.ERROR
        else -> Screen.DISCONNECTED
    }
}
