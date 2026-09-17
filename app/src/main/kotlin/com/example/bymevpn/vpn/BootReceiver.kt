package com.example.bymevpn.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.bymevpn.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered on system boot to honor the user's "Auto-connect on Boot" setting.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = SettingsRepository.getInstance(context).settingsFlow.first()
                    if (settings.autoConnectOnBoot) {
                        Log.d("BootReceiver", "Auto-connect on boot is enabled, starting VPN...")
                        if (VpnManager.isVpnPrepared(context)) {
                            VpnManager.startVpn(context)
                        } else {
                            Log.w("BootReceiver", "VPN permission not granted yet by user, skipping auto-connect on boot")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error during boot receiver execution: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
