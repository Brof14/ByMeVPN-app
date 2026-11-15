package com.example.bymevpn.vpn

/** Restore only an explicitly active session or an Android-managed Always-on VPN. */
internal object VpnServiceRestorePolicy {
    fun shouldRestore(previouslyActive: Boolean, systemAlwaysOn: Boolean): Boolean =
        previouslyActive || systemAlwaysOn
}
