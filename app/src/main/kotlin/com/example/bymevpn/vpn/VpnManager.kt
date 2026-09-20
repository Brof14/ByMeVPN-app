package com.example.bymevpn.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.settings.SettingsRepository
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.example.bymevpn.vpn.xray.XrayVpnEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-level VPN facade coordinating UI states, server selection, and Xray VLESS+Reality lifecycle.
 */
class VpnManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "VpnManager"

        @Volatile
        private var INSTANCE: VpnManager? = null

        fun getInstance(context: Context): VpnManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VpnManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun isVpnPrepared(context: Context): Boolean {
            return VpnService.prepare(context) == null
        }

        fun getVpnPrepareIntent(context: Context): Intent? = VpnService.prepare(context)

        fun startVpn(context: Context, server: ServerNode? = null) {
            val manager = getInstance(context)
            server?.let { manager.selectServer(it) }
            manager.connect()
        }

        fun stopVpn(context: Context) {
            getInstance(context).disconnect()
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val apiClient = ByMeApiClient(context)
    private val subscriptionManager = SubscriptionManager.getInstance(context)
    private val settingsRepo = SettingsRepository.getInstance(context)
    private val engine = XrayVpnEngine.getInstance(context)

    private val _state = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    private val _currentServer = MutableStateFlow<ServerNode?>(null)
    val currentServer: StateFlow<ServerNode?> = _currentServer.asStateFlow()

    private val _availableServers = MutableStateFlow<List<ServerNode>>(emptyList())
    val availableServers: StateFlow<List<ServerNode>> = _availableServers.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _bytesIn = MutableStateFlow(0L)
    val bytesIn: StateFlow<Long> = _bytesIn.asStateFlow()

    private val _bytesOut = MutableStateFlow(0L)
    val bytesOut: StateFlow<Long> = _bytesOut.asStateFlow()

    private val _connectedSeconds = MutableStateFlow(0L)
    val connectedSeconds: StateFlow<Long> = _connectedSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var engineObserverJob: Job? = null

    init {
        // Observe engine statistics
        scope.launch {
            engine.statistics.collect { stats ->
                _bytesIn.value = stats.bytesIn
                _bytesOut.value = stats.bytesOut
            }
        }
    }

    suspend fun loadServers(): Result<List<ServerNode>> = withContext(Dispatchers.IO) {
        try {
            val servers = apiClient.getServers()
            _availableServers.value = servers
            if (_currentServer.value == null && servers.isNotEmpty()) {
                _currentServer.value = servers.first()
            }
            Result.success(servers)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load servers from API: ${e.message}")
            Result.failure(e)
        }
    }

    fun selectServer(server: ServerNode) {
        val prevServer = _currentServer.value
        _currentServer.value = server

        // If currently connected, reconnect to the newly selected server seamlessly
        if (_state.value == VpnConnectionState.CONNECTED && prevServer?.nodeCode != server.nodeCode) {
            scope.launch {
                disconnect()
                delay(500)
                connect()
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun connect() {
        if (_state.value == VpnConnectionState.CONNECTING || _state.value == VpnConnectionState.CONNECTED) {
            return
        }

        scope.launch {
            _state.value = VpnConnectionState.CONNECTING
            _errorMessage.value = null

            try {
                // 1. Verify subscription entitlement
                if (!subscriptionManager.canConnect()) {
                    val refreshed = subscriptionManager.refreshStatus()
                    if (refreshed.isFailure || !subscriptionManager.canConnect()) {
                        _state.value = VpnConnectionState.ERROR
                        _errorMessage.value = "SUBSCRIPTION_REQUIRED"
                        return@launch
                    }
                }

                // 2. Ensure server is selected
                var targetServer = _currentServer.value
                if (targetServer == null) {
                    val servers = apiClient.getServers()
                    _availableServers.value = servers
                    targetServer = servers.firstOrNull()
                        ?: throw IllegalStateException(
                            if (com.example.bymevpn.data.LocaleManager.isRussian(context))
                                "Не удалось получить список серверов"
                            else
                                "No servers available"
                        )
                    _currentServer.value = targetServer
                }

                // 3. Obtain VLESS+Reality session configuration from backend
                val sessionConfig = try {
                    apiClient.createVpnSession(targetServer.nodeCode)
                } catch (e: Exception) {
                    val isRu = com.example.bymevpn.data.LocaleManager.isRussian(context)
                    val rawMsg = e.message ?: ""
                    val msg = when {
                        rawMsg.contains("NODE_PROVISIONING_NOT_CONFIGURED", ignoreCase = true) ||
                        rawMsg.contains("not configured", ignoreCase = true) -> {
                            if (isRu) "VPN-узлы сейчас настраиваются на сервере (VPN недоступен)"
                            else "VPN nodes are currently being configured on server (VPN unavailable)"
                        }
                        rawMsg.contains("503") -> {
                            if (isRu) "Сервис VPN временно недоступен (503 Service Unavailable)"
                            else "VPN service temporarily unavailable (503)"
                        }
                        else -> {
                            val prefix = if (isRu) "Не удалось получить конфигурацию сервера" else "Failed to get server configuration"
                            val detail = if (rawMsg.isNotBlank()) rawMsg else if (isRu) "ошибка сети" else "network error"
                            "$prefix ($detail)"
                        }
                    }
                    throw RuntimeException(msg, e)
                }

                // 4. Start foreground Android VpnService
                ByMeVpnService.start(
                    context = context,
                    serverName = targetServer.city.ifEmpty { targetServer.country },
                    serverCountry = targetServer.country
                )

                // 5. Start Xray VPN engine (will report BLOCKED if native engine is not compiled into build)
                val engineResult = engine.startTunnel(sessionConfig)
                if (engineResult.isFailure) {
                    val ex = engineResult.exceptionOrNull()
                    val isRu = com.example.bymevpn.data.LocaleManager.isRussian(context)
                    val msg = ex?.message ?: if (isRu) "VPN-движок недоступен в этой сборке" else "VPN engine unavailable in this build"
                    throw RuntimeException(msg, ex)
                }

                _state.value = VpnConnectionState.CONNECTED
                startTimer()

            } catch (e: Exception) {
                Log.e(TAG, "Connect failed: ${e.message}", e)
                ByMeVpnService.stop(context)
                _state.value = VpnConnectionState.ERROR
                _errorMessage.value = e.message ?: (if (com.example.bymevpn.data.LocaleManager.isRussian(context)) "Ошибка подключения" else "Connection error")
            }
        }
    }

    fun disconnect() {
        if (_state.value == VpnConnectionState.DISCONNECTED || _state.value == VpnConnectionState.DISCONNECTING) {
            return
        }

        scope.launch {
            _state.value = VpnConnectionState.DISCONNECTING
            try {
                engine.disconnect()
                ByMeVpnService.stop(context)
                apiClient.deleteVpnSession()
            } catch (e: Exception) {
                Log.w(TAG, "Disconnect cleanup warning: ${e.message}")
            } finally {
                _state.value = VpnConnectionState.DISCONNECTED
                stopTimer()
            }
        }
    }

    fun reconnect() {
        scope.launch {
            disconnect()
            delay(500)
            connect()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _connectedSeconds.value = 0L
        timerJob = scope.launch {
            while (isActive && _state.value == VpnConnectionState.CONNECTED) {
                delay(1000)
                _connectedSeconds.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
