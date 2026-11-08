package com.example.bymevpn.vpn

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.bymevpn.MainActivity
import com.example.bymevpn.data.storage.SecureTokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Android Quick Settings Tile for 1-tap VPN toggle directly from notifications pull-down.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ByMeVpnTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateObserver: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
        stateObserver?.cancel()
        stateObserver = serviceScope.launch {
            VpnManager.getInstance(applicationContext).state.collect { updateTileState() }
        }
    }

    override fun onStopListening() {
        stateObserver?.cancel()
        stateObserver = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val vpnManager = VpnManager.getInstance(applicationContext)
        val state = vpnManager.state.value

        when (state) {
            VpnConnectionState.CONNECTED, VpnConnectionState.CONNECTING -> {
                vpnManager.disconnect()
                updateTileState(Tile.STATE_INACTIVE)
            }
            VpnConnectionState.DISCONNECTED, VpnConnectionState.ERROR, VpnConnectionState.DISCONNECTING -> {
                if (VpnManager.isVpnPrepared(applicationContext) &&
                    SecureTokenStorage.getInstance(applicationContext).isVpnDisclosureAccepted()) {
                    VpnManager.startVpn(applicationContext)
                    // The tile reflects tunnel callbacks, not an optimistic connection attempt.
                    updateTileState(Tile.STATE_INACTIVE)
                } else {
                    // Open main activity to request system VPN permission
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val pendingIntent = PendingIntent.getActivity(
                            applicationContext, 0, intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        startActivityAndCollapse(pendingIntent)
                    } else {
                        @Suppress("DEPRECATION")
                        startActivityAndCollapse(intent)
                    }
                }
            }
        }
    }

    private fun updateTileState(forcedState: Int? = null) {
        val tile = qsTile ?: return
        val isRunning = VpnManager.getInstance(applicationContext).state.value == VpnConnectionState.CONNECTED
        val targetState = forcedState ?: if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        tile.state = targetState
        tile.label = "ByMeVPN"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (targetState == Tile.STATE_ACTIVE) "Защищено" else "Отключено"
        }
        tile.updateTile()
    }
}
