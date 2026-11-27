package com.example.bymevpn.vpn

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.bymevpn.MainActivity
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.subscription.SubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android Quick Settings Tile for 1-tap VPN toggle directly from notifications pull-down.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ByMeVpnTileService : TileService() {

    companion object {
        fun requestTileSync(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val appContext = context.applicationContext
                runCatching {
                    requestListeningState(
                        appContext,
                        ComponentName(appContext, ByMeVpnTileService::class.java)
                    )
                }
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
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

    override fun onDestroy() {
        stateObserver?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { handleTileClick() }
        } else {
            handleTileClick()
        }
    }

    private fun handleTileClick() {
        val vpnManager = VpnManager.getInstance(applicationContext)
        val state = vpnManager.state.value

        when (state) {
            VpnConnectionState.CONNECTED, VpnConnectionState.CONNECTING -> {
                vpnManager.disconnect()
                updateTileState()
            }
            VpnConnectionState.DISCONNECTED, VpnConnectionState.ERROR, VpnConnectionState.DISCONNECTING -> {
                val canStart = VpnManager.isVpnPrepared(applicationContext) &&
                    SecureTokenStorage.getInstance(applicationContext).isVpnDisclosureAccepted() &&
                    SubscriptionManager.getInstance(applicationContext).canConnect()
                if (canStart) {
                    VpnManager.startVpn(applicationContext)
                    updateTileState()
                } else {
                    openAppFromTile()
                }
            }
        }
    }

    private fun openAppFromTile() {
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
            startActivityAndCollapseLegacy(intent)
        }
    }

    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun startActivityAndCollapseLegacy(intent: Intent) {
        startActivityAndCollapse(intent)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val state = VpnManager.getInstance(applicationContext).state.value
        val isRu = LocaleManager.isRussian(applicationContext)

        tile.state = when (state) {
            VpnConnectionState.CONNECTED, VpnConnectionState.CONNECTING -> Tile.STATE_ACTIVE
            VpnConnectionState.DISCONNECTED, VpnConnectionState.DISCONNECTING, VpnConnectionState.ERROR -> Tile.STATE_INACTIVE
        }
        tile.label = "ByMeVPN"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (state) {
                VpnConnectionState.CONNECTED -> if (isRu) "Защищено" else "Protected"
                VpnConnectionState.CONNECTING -> if (isRu) "Подключение…" else "Connecting…"
                VpnConnectionState.DISCONNECTING -> if (isRu) "Отключение…" else "Disconnecting…"
                VpnConnectionState.ERROR -> if (isRu) "Ошибка" else "Error"
                VpnConnectionState.DISCONNECTED -> if (isRu) "Отключено" else "Disconnected"
            }
        }
        tile.updateTile()
    }
}
