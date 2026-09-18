package com.example.bymevpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.bymevpn.MainActivity
import com.example.bymevpn.R
import com.example.bymevpn.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Standard Android VpnService for ByMeVPN.
 * Configures system TUN interface, DNS, MTU, split-tunneling (addDisallowedApplication),
 * and foreground lifecycle notifications for Xray VLESS + Reality.
 */
class ByMeVpnService : VpnService() {

    companion object {
        private const val TAG = "ByMeVpnService"
        const val ACTION_CONNECT = "com.example.bymevpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.bymevpn.ACTION_DISCONNECT"

        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_COUNTRY = "extra_server_country"

        const val NOTIFICATION_ID = 2024
        const val CHANNEL_ID = "bymevpn_active_tunnel"

        fun start(context: Context, serverName: String = "", serverCountry: String = "") {
            val intent = Intent(context, ByMeVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_SERVER_NAME, serverName)
                putExtra(EXTRA_SERVER_COUNTRY, serverCountry)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting VPN service: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ByMeVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping VPN service: ${e.message}", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var tunInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Global"
                val serverCountry = intent.getStringExtra(EXTRA_SERVER_COUNTRY) ?: ""

                val notif = buildNotification(
                    title = if (serverCountry.isNotEmpty()) "ByMeVPN • $serverCountry" else "ByMeVPN",
                    text = "VLESS + Reality: $serverName"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notif)
                }

                serviceScope.launch {
                    establishTunInterface()
                }
            }
            ACTION_DISCONNECT -> {
                tearDownTunInterface()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Builds and configures system TUN interface according to VLESS + Reality standards,
     * applying MTU, routes, DNS, and Split-tunneling rules.
     */
    private suspend fun establishTunInterface() {
        try {
            val builder = Builder()
                .setSession("ByMeVPN (VLESS + Reality)")
                .setMtu(1500)
                .addAddress("10.233.233.2", 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")

            // Apply Split-tunneling if enabled in AppSettings
            val settingsRepo = SettingsRepository.getInstance(applicationContext)
            val settings = settingsRepo.settingsFlow.first()

            if (settings.splitTunnelingEnabled && settings.excludedApps.isNotEmpty()) {
                val pm = packageManager
                for (pkg in settings.excludedApps) {
                    try {
                        pm.getPackageInfo(pkg, 0)
                        builder.addDisallowedApplication(pkg)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w(TAG, "Split tunneling: excluded app not installed: $pkg")
                    }
                }
            }

            // Exclude self from VPN tunnel to prevent loops
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not disallow self package: ${e.message}")
            }

            tunInterface?.close()
            tunInterface = builder.establish()
            Log.i(TAG, "TUN interface established successfully: fd=${tunInterface?.fd}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish TUN interface: ${e.message}", e)
        }
    }

    private fun tearDownTunInterface() {
        try {
            tunInterface?.close()
            tunInterface = null
            Log.i(TAG, "TUN interface closed successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing TUN interface: ${e.message}")
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val disconnectIntent = Intent(this, ByMeVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                disconnectPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ByMeVPN Tunnel Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active VLESS + Reality VPN tunnel notification"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onRevoke() {
        VpnManager.getInstance(applicationContext).disconnect()
        tearDownTunInterface()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        tearDownTunInterface()
        super.onDestroy()
    }
}
