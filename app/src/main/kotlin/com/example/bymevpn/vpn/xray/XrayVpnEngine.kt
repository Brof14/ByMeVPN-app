package com.example.bymevpn.vpn.xray

import android.content.Context
import android.util.Log
import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.VpnConnectionState
import com.example.bymevpn.vpn.VpnEngine
import com.example.bymevpn.vpn.VpnStatistics
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real Xray-core VpnEngine implementation for Android.
 * Integrates libv2ray (native Xray-core Go library) to manage VLESS + Reality tunnels
 * over the system TUN file descriptor.
 */
class XrayVpnEngine private constructor(private val context: Context) : VpnEngine {

    companion object {
        private const val TAG = "XrayVpnEngine"

        @Volatile
        private var INSTANCE: XrayVpnEngine? = null

        fun getInstance(context: Context): XrayVpnEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: XrayVpnEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _state = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    override val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    private val _statistics = MutableStateFlow(VpnStatistics())
    override val statistics: StateFlow<VpnStatistics> = _statistics.asStateFlow()

    private val envInitialized = AtomicBoolean(false)
    private var coreController: CoreController? = null
    private var activeConfig: VpnSessionConfig? = null
    private var statsJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.IO)

    override val isRunning: Boolean
        get() = (coreController?.isRunning == true) && (_state.value == VpnConnectionState.CONNECTED)

    /**
     * Initializes libv2ray native environment and routing assets once.
     */
    fun initCoreEnv() {
        if (envInitialized.compareAndSet(false, true)) {
            try {
                Seq.setContext(context.applicationContext)
                val assetDir = context.getDir("assets", Context.MODE_PRIVATE)
                copyAssetsToStorage(context, assetDir)
                Libv2ray.initCoreEnv(assetDir.absolutePath, "ByMeVPN_DeviceId")
                val version = Libv2ray.checkVersionX()
                Log.i(TAG, "Xray core environment initialized. Version: $version")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize Xray core env: ${e.message}", e)
                envInitialized.set(false)
            }
        }
    }

    private fun copyAssetsToStorage(context: Context, destDir: File) {
        val assetManager = context.assets
        listOf("geoip.dat", "geosite.dat").forEach { assetName ->
            try {
                val outFile = File(destDir, assetName)
                if (!outFile.exists() || outFile.length() == 0L) {
                    assetManager.open(assetName).use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Extracted asset: $assetName to ${outFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Asset $assetName not extracted: ${e.message}")
            }
        }
    }

    /**
     * Starts the Xray-core processing loop with the provided VLESS + Reality session config and TUN fd.
     * Only returns success if the coreController confirms isRunning == true.
     */
    suspend fun startTunnel(config: VpnSessionConfig, tunFd: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = VpnConnectionState.CONNECTING
            activeConfig = config

            if (config.server.isBlank() || config.uuid.isBlank() || config.publicKey.isBlank()) {
                throw IllegalArgumentException("Invalid VLESS configuration: server, uuid, or publicKey missing")
            }

            initCoreEnv()

            val xrayConfigJson = XrayConfigGenerator.generateString(config, bypassRussianTraffic = true)
            Log.d(TAG, "Starting Xray-core for ${config.server}:${config.port} (tunFd=$tunFd)")

            // Stop any currently running instance first
            stopCoreInternal()

            val callback = object : CoreCallbackHandler {
                override fun startup(): Long {
                    Log.i(TAG, "CoreCallback: startup")
                    return 0
                }

                override fun shutdown(): Long {
                    Log.i(TAG, "CoreCallback: shutdown")
                    return 0
                }

                override fun onEmitStatus(status: Long, msg: String?): Long {
                    Log.d(TAG, "CoreCallback status: $status, $msg")
                    return 0
                }
            }

            val controller = Libv2ray.newCoreController(callback)
            coreController = controller

            // Start loop in Xray core passing config JSON and TUN fd
            controller.startLoop(xrayConfigJson, tunFd)

            // Verify the core actually started and is running
            if (!controller.isRunning) {
                _state.value = VpnConnectionState.ERROR
                throw IllegalStateException("Xray core loop failed to start (isRunning=false)")
            }

            Log.i(TAG, "Xray-core started successfully. isRunning: ${controller.isRunning}")
            _state.value = VpnConnectionState.CONNECTED
            startStatsCollector()
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start Xray tunnel: ${e.message}", e)
            _state.value = VpnConnectionState.ERROR
            stopCoreInternal()
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        _state.value = VpnConnectionState.DISCONNECTING
        stopStatsCollector()
        stopCoreInternal()
        activeConfig = null
        _statistics.value = VpnStatistics()
        _state.value = VpnConnectionState.DISCONNECTED
        Log.i(TAG, "Xray tunnel stopped")
        Unit
    }

    override suspend fun reconnect(): Unit = withContext(Dispatchers.IO) {
        // Reconnect handled via VpnManager
        Unit
    }

    private fun stopCoreInternal() {
        try {
            coreController?.let { controller ->
                if (controller.isRunning) {
                    controller.stopLoop()
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error stopping core loop: ${e.message}")
        } finally {
            coreController = null
        }
    }

    private fun startStatsCollector() {
        stopStatsCollector()
        statsJob = engineScope.launch {
            var lastUp = 0L
            var lastDown = 0L
            var lastTime = System.currentTimeMillis()

            while (isActive && isRunning) {
                delay(2000)
                try {
                    val rawStats = coreController?.queryAllOutboundTrafficStats() ?: ""
                    var totalUp = 0L
                    var totalDown = 0L
                    if (rawStats.isNotBlank()) {
                        rawStats.split(";").forEach { entry ->
                            val parts = entry.split(",")
                            if (parts.size == 3) {
                                val direction = parts[1]
                                val value = parts[2].toLongOrNull() ?: 0L
                                if (direction.contains("uplink", ignoreCase = true)) {
                                    totalUp += value
                                } else if (direction.contains("downlink", ignoreCase = true)) {
                                    totalDown += value
                                }
                            }
                        }
                    }

                    val now = System.currentTimeMillis()
                    val durationSec = ((now - lastTime) / 1000.0).coerceAtLeast(1.0)
                    val speedUp = ((totalUp - lastUp).coerceAtLeast(0L) / durationSec).toLong()
                    val speedDown = ((totalDown - lastDown).coerceAtLeast(0L) / durationSec).toLong()

                    lastUp = totalUp
                    lastDown = totalDown
                    lastTime = now

                    _statistics.value = VpnStatistics(
                        bytesIn = totalDown,
                        bytesOut = totalUp,
                        downloadSpeedBps = speedDown,
                        uploadSpeedBps = speedUp
                    )
                } catch (e: Throwable) {
                    Log.v(TAG, "Stats collection error: ${e.message}")
                }
            }
        }
    }

    private fun stopStatsCollector() {
        statsJob?.cancel()
        statsJob = null
    }
}
