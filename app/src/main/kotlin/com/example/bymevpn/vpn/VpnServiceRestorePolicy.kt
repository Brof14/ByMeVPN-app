package com.example.bymevpn.vpn

/** Restore only an explicitly active session or an Android-managed Always-on VPN. */
internal object VpnServiceRestorePolicy {
    enum class Outcome { RESTORED, FAILED, CANCELLED }
    /** Account and endpoint refresh must not keep the system foreground service indefinitely. */
    const val RESTORE_TIMEOUT_MS = 120_000L

    fun shouldRestore(previouslyActive: Boolean, systemAlwaysOn: Boolean): Boolean =
        previouslyActive || systemAlwaysOn

    fun outcomeFor(state: VpnConnectionState): Outcome? = when (state) {
        VpnConnectionState.CONNECTED -> Outcome.RESTORED
        VpnConnectionState.ERROR -> Outcome.FAILED
        VpnConnectionState.DISCONNECTED -> Outcome.CANCELLED
        VpnConnectionState.CONNECTING, VpnConnectionState.DISCONNECTING -> null
    }
}
