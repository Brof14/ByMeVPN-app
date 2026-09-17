package com.example.bymevpn.vpn

import android.content.Context
import android.net.VpnService
import com.example.bymevpn.data.api.ServerNode
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level VPN facade unifying UI state with WireGuardVpnManager and Android VpnService.
 */
object VpnManager {

    fun isVpnPrepared(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    fun getVpnPrepareIntent(context: Context) = VpnService.prepare(context)

    fun startVpn(context: Context, server: ServerNode? = null) {
        val manager = WireGuardVpnManager.getInstance(context)
        server?.let { manager.selectServer(it) }
        ByMeVpnService.start(context)
        manager.connect()
    }

    fun stopVpn(context: Context) {
        WireGuardVpnManager.getInstance(context).disconnect()
        ByMeVpnService.stop(context)
    }
}
