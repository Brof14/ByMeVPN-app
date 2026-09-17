package com.example.bymevpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.bymevpn.MainActivity
import com.example.bymevpn.R
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.data.settings.SettingsRepository
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetEndpoint
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.util.Locale

/**
 * Official WireGuard VpnService for ByMeVPN.
 * Runs official GoBackend tunnel under Android VpnService lifecycle.
 */
class ByMeVpnService : VpnService() {

    companion object {
        private const val TAG = "ByMeVpnService"
        const val ACTION_CONNECT = "com.example.bymevpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.bymevpn.ACTION_DISCONNECT"

        const val NOTIFICATION_ID = 2024
        const val CHANNEL_ID = "bymevpn_active_tunnel"

        fun start(context: Context) {
            val intent = Intent(context, ByMeVpnService::class.java).apply {
                action = ACTION_CONNECT
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
    private var goBackend: GoBackend? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            goBackend = GoBackend(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed initializing GoBackend in service: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                val initialNotification = buildNotification(
                    title = "ByMeVPN",
                    text = "Establishing WireGuard encrypted tunnel..."
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
                } else {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }
            }
            ACTION_DISCONNECT -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(title: String, text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
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
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ByMeVPN Tunnel Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active WireGuard VPN tunnel connection state"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onRevoke() {
        WireGuardVpnManager.getInstance(applicationContext).disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onRevoke()
    }
}
