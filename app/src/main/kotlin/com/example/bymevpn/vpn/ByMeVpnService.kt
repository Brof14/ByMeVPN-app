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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.Locale

class ByMeVpnService : VpnService() {

    companion object {
        private const val TAG = "ByMeVpnService"
        const val ACTION_CONNECT = "com.example.bymevpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.bymevpn.ACTION_DISCONNECT"
        const val EXTRA_SERVER_COUNTRY = "EXTRA_SERVER_COUNTRY"
        const val EXTRA_SERVER_CITY = "EXTRA_SERVER_CITY"
        const val EXTRA_SERVER_IP = "EXTRA_SERVER_IP"
        const val EXTRA_SERVER_CODE = "EXTRA_SERVER_CODE"
        const val EXTRA_SERVER_PING = "EXTRA_SERVER_PING"

        const val NOTIFICATION_ID = 2024
        const val CHANNEL_ID = "bymevpn_active_tunnel"

        fun start(context: Context, server: VpnServer) {
            val intent = Intent(context, ByMeVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_SERVER_COUNTRY, server.country)
                putExtra(EXTRA_SERVER_CITY, server.city)
                putExtra(EXTRA_SERVER_IP, server.ip)
                putExtra(EXTRA_SERVER_CODE, server.countryCode)
                putExtra(EXTRA_SERVER_PING, server.ping)
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

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var tunnelJob: Job? = null
    private var timerJob: Job? = null
    private var secondsConnected: Long = 0L
    private var currentServer: VpnServer = AVAILABLE_SERVERS[0]
    private var packetRouter: VpnPacketRouter? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                val country = intent.getStringExtra(EXTRA_SERVER_COUNTRY) ?: "Netherlands"
                val city = intent.getStringExtra(EXTRA_SERVER_CITY) ?: "Amsterdam"
                val ip = intent.getStringExtra(EXTRA_SERVER_IP) ?: "18.110.221.14"
                val code = intent.getStringExtra(EXTRA_SERVER_CODE) ?: "NL"
                val ping = intent.getIntExtra(EXTRA_SERVER_PING, 12)

                currentServer = VpnServer(country, city, code, ip, ping)
                startVpnTunnel(currentServer)
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startVpnTunnel(server: VpnServer) {
        // Post foreground notification immediately as required by Android
        val notification = buildNotification("Connecting to ${server.city}...", "Setting up secure tunnel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        tunnelJob?.cancel()
        timerJob?.cancel()

        tunnelJob = serviceScope.launch {
            try {
                // Short simulation of handshake
                delay(300)

                // Close any existing interface
                vpnInterface?.close()
                vpnInterface = null

                // Establish native Android TUN interface
                val builder = Builder()
                    .setSession("ByMeVPN - ${server.country}")
                    .addAddress("10.8.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .setMtu(1500)
                    .setBlocking(false)

                // Prevent VPN routing loops for the app itself
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.w(TAG, "addDisallowedApplication failed: ${e.message}")
                }

                // Route web traffic through local transparent proxy on Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        builder.setHttpProxy(
                            android.net.ProxyInfo.buildDirectProxy("127.0.0.1", VpnPacketRouter.PROXY_PORT)
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "setHttpProxy failed: ${e.message}")
                    }
                }

                val pfd = builder.establish()
                if (pfd == null) {
                    Log.e(TAG, "VpnService.Builder.establish() returned null")
                    VpnManager.onServiceError("Failed to establish TUN interface")
                    stopSelf()
                    return@launch
                }

                vpnInterface = pfd

                // Start packet router (handles ICMP Ping, UDP DNS, and local proxy)
                packetRouter?.stop()
                val router = VpnPacketRouter(this@ByMeVpnService, pfd.fileDescriptor, serviceScope, server)
                packetRouter = router
                router.start()

                VpnManager.onServiceConnected(server)

                // Start timer & notification updates
                secondsConnected = 0L
                startTimer(server)
            } catch (e: Exception) {
                Log.e(TAG, "Tunnel execution error: ${e.message}", e)
                VpnManager.onServiceError(e.localizedMessage ?: "VPN Tunnel error")
                stopSelf()
            }
        }
    }

    private fun startTimer(server: VpnServer) {
        timerJob = serviceScope.launch {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            while (isActive && vpnInterface != null) {
                delay(1000L)
                secondsConnected++
                VpnManager.updateDuration(secondsConnected)

                // Update notification periodically
                if (secondsConnected % 5 == 0L) {
                    val formattedTime = formatDuration(secondsConnected)
                    val updatedNotification = buildNotification(
                        title = "Connected • ${server.country} (${server.city})",
                        text = "Encrypted tunnel active [$formattedTime] • IP: ${server.ip}"
                    )
                    notificationManager?.notify(NOTIFICATION_ID, updatedNotification)
                }
            }
        }
    }

    private fun stopVpnTunnel() {
        tunnelJob?.cancel()
        timerJob?.cancel()

        try {
            packetRouter?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping packetRouter: ${e.message}", e)
        } finally {
            packetRouter = null
        }

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing vpn interface: ${e.message}", e)
        } finally {
            vpnInterface = null
        }

        VpnManager.onServiceDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        // Called by Android system if user disconnects VPN from OS settings
        stopVpnTunnel()
        stopSelf()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ByMeVPN Connection Status"
            val descriptionText = "Displays ongoing VPN status and duration"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, ByMeVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val pendingDisconnect = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", pendingDisconnect)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun formatDuration(totalSecs: Long): String {
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
    }
}
