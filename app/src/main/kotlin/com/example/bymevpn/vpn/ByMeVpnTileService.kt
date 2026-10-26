package com.example.bymevpn.vpn

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.bymevpn.MainActivity
import com.example.bymevpn.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Android Quick Settings Tile for 1-tap VPN toggle directly from notifications pull-down.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ByMeVpnTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
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
            VpnConnectionState.DISCONNECTED, VpnConnectionState.ERROR -> {
                if (VpnManager.isVpnPrepared(applicationContext)) {
                    VpnManager.startVpn(applicationContext)
                    updateTileState(Tile.STATE_ACTIVE)
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
        val isRunning = ByMeVpnService.isServiceRunning
        val targetState = forcedState ?: if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        tile.state = targetState
        tile.label = "ByMeVPN"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (targetState == Tile.STATE_ACTIVE) "Защищено" else "Отключено"
        }
        tile.updateTile()
    }
}
