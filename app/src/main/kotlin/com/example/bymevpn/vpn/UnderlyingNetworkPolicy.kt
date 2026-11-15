package com.example.bymevpn.vpn

/** A VPN transport can report INTERNET while its physical route is gone. */
internal object UnderlyingNetworkPolicy {
    fun isUsable(hasInternet: Boolean, isVpn: Boolean, hasPhysicalTransport: Boolean): Boolean =
        hasInternet && !isVpn && hasPhysicalTransport
}
