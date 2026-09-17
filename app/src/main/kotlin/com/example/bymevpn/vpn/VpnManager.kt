package com.example.bymevpn.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object VpnManager {
    private val _sessionState = MutableStateFlow(VpnSessionState())
    val sessionState: StateFlow<VpnSessionState> = _sessionState.asStateFlow()

    /**
     * Checks if Android OS requires the user to approve the VPN permission dialog.
     * Returns null if already granted, or the Intent to launch the system dialog if not.
     */
    fun getVpnPrepareIntent(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    fun isVpnPrepared(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    fun selectServer(server: VpnServer) {
        _sessionState.update { it.copy(selectedServer = server) }
    }

    fun startVpn(context: Context, server: VpnServer = _sessionState.value.selectedServer) {
        _sessionState.update {
            it.copy(
                isConnecting = true,
                selectedServer = server,
                errorMessage = null
            )
        }
        ByMeVpnService.start(context, server)
    }

    fun stopVpn(context: Context) {
        _sessionState.update {
            it.copy(
                isConnecting = false,
                isConnected = false
            )
        }
        ByMeVpnService.stop(context)
    }

    internal fun onServiceConnected(server: VpnServer) {
        _sessionState.update {
            it.copy(
                isConnected = true,
                isConnecting = false,
                selectedServer = server,
                durationSeconds = 0L,
                errorMessage = null
            )
        }
    }

    internal fun onServiceDisconnected() {
        _sessionState.update {
            it.copy(
                isConnected = false,
                isConnecting = false
            )
        }
    }

    internal fun onServiceError(message: String) {
        _sessionState.update {
            it.copy(
                isConnected = false,
                isConnecting = false,
                errorMessage = message
            )
        }
    }

    internal fun updateDuration(seconds: Long) {
        _sessionState.update {
            it.copy(durationSeconds = seconds)
        }
    }

    internal fun recordTraffic(bytesSent: Long = 0, bytesReceived: Long = 0) {
        _sessionState.update {
            it.copy(
                bytesSent = it.bytesSent + bytesSent,
                bytesReceived = it.bytesReceived + bytesReceived
            )
        }
    }
}
