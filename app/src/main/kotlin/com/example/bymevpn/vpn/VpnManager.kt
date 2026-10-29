package com.example.bymevpn.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.settings.SettingsRepository
import com.example.bymevpn.data.storage.SecureTokenStorage
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
    private val storage = SecureTokenStorage.getInstance(context)
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

    private val _downloadSpeedBps = MutableStateFlow(0L)
    val downloadSpeedBps: StateFlow<Long> = _downloadSpeedBps.asStateFlow()

    private val _uploadSpeedBps = MutableStateFlow(0L)
    val uploadSpeedBps: StateFlow<Long> = _uploadSpeedBps.asStateFlow()

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
                _downloadSpeedBps.value = stats.downloadSpeedBps
                _uploadSpeedBps.value = stats.uploadSpeedBps
            }
        }
    }

    suspend fun autoSelectFastestServer(): ServerNode? = withContext(Dispatchers.IO) {
        val servers = _availableServers.value
        if (servers.isEmpty()) return@withContext null

        val storage = SecureTokenStorage.getInstance(context)
        val targets = servers.mapNotNull { node ->
            val cfg = storage.getServerConfig(node.nodeCode)
            if (cfg != null) node.nodeCode to Pair(cfg.server, cfg.port) else null
        }

        val pings = if (targets.isNotEmpty()) {
            com.example.bymevpn.data.PingTester.measureAllConcurrent(targets, timeoutMs = 1500)
        } else {
            emptyMap()
        }

        val bestNode = if (pings.isNotEmpty()) {
            val realityPinged = servers.filter { it.protocol.contains("reality", ignoreCase = true) && (pings[it.nodeCode] ?: 9999) > 0 }
            if (realityPinged.isNotEmpty()) {
                realityPinged.minByOrNull { pings[it.nodeCode] ?: 9999 }
            } else {
                val minEntry = pings.minByOrNull { it.value }
                servers.find { it.nodeCode == minEntry?.key }
            }
        } else null

        val winner = bestNode ?: servers.firstOrNull { it.protocol.contains("reality", ignoreCase = true) } ?: servers.firstOrNull()
        winner?.also {
            withContext(Dispatchers.Main) {
                selectServer(it)
            }
        }
    }

    suspend fun refreshAllPings(): Map<String, Int> = withContext(Dispatchers.IO) {
        val servers = _availableServers.value
        if (servers.isEmpty()) return@withContext emptyMap()

        val storage = SecureTokenStorage.getInstance(context)
        val targets = servers.mapNotNull { node ->
            val cfg = storage.getServerConfig(node.nodeCode)
            if (cfg != null && cfg.server.isNotBlank()) {
                val host = if (cfg.server.equals("bymevpn.duckdns.org", true)) "2.27.120.247" else cfg.server
                node.nodeCode to Pair(host, cfg.port)
            } else null
        }

        if (targets.isEmpty()) return@withContext emptyMap()
        val pings = com.example.bymevpn.data.PingTester.measureAllConcurrent(targets, timeoutMs = 1500)
        if (pings.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                val updated = servers.map { node ->
                    val p = pings[node.nodeCode]
                    if (p != null && p > 0) node.copy(pingMs = p) else node
                }
                _availableServers.value = updated
                val cur = _currentServer.value
                if (cur != null) {
                    val curPing = pings[cur.nodeCode]
                    if (curPing != null && curPing > 0) {
                        _currentServer.value = cur.copy(pingMs = curPing)
                    }
                }
            }
        }
        pings
    }

    fun setServers(servers: List<ServerNode>) {
        _availableServers.value = servers
        val preferred = servers.firstOrNull { storage.getServerConfig(it.nodeCode)?.security == "reality" } ?: servers.firstOrNull()
        if (_currentServer.value == null || !servers.any { it.nodeCode == _currentServer.value?.nodeCode }) {
            _currentServer.value = preferred
        }
        scope.launch {
            refreshAllPings()
        }
    }

    suspend fun loadServers(): Result<List<ServerNode>> = withContext(Dispatchers.IO) {
        try {
            // 1. Load from subscription URL if set
            val subUrl = storage.getSubscriptionUrl()
            if (!subUrl.isNullOrBlank()) {
                val parsed = subscriptionManager.applySubscriptionUrl(subUrl).getOrNull()
                if (parsed != null && parsed.servers.isNotEmpty()) {
                    setServers(parsed.servers)
                    return@withContext Result.success(parsed.servers)
                }
            }

            // 2. Fetch directly from backend API
            val servers = try {
                apiClient.getServers()
            } catch (e: Exception) {
                emptyList()
            }
            if (servers.isNotEmpty()) {
                setServers(servers)
                return@withContext Result.success(servers)
            }

            // 3. Fallback to existing valid cached configs in local storage
            val cachedConfigs = storage.getServerConfigs()
            if (cachedConfigs.isNotEmpty()) {
                val localNodes = cachedConfigs.map { (code, cfg) ->
                    ServerNode(
                        nodeCode = code,
                        country = "Нидерланды",
                        countryCode = "nl",
                        city = "ByMeVPN",
                        flag = "🇳🇱",
                        pingMs = 25,
                        loadPercent = 20,
                        isPremium = false,
                        countryEn = "Netherlands",
                        cityEn = "ByMeVPN"
                    )
                }
                setServers(localNodes)
                return@withContext Result.success(localNodes)
            }

            _availableServers.value = emptyList()
            _currentServer.value = null
            Result.failure(Exception("Нет доступных серверов. Активируйте подписку или войдите через Telegram-бота."))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load servers: ${e.message}")
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
                    subscriptionManager.refreshStatus()
                    if (!subscriptionManager.canConnect()) {
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
                    targetServer = servers.firstOrNull { storage.getServerConfig(it.nodeCode)?.security == "reality" } ?: servers.firstOrNull()
                        ?: throw IllegalStateException(
                            if (com.example.bymevpn.data.LocaleManager.isRussian(context))
                                "Не удалось получить список серверов"
                            else
                                "No servers available"
                        )
                    _currentServer.value = targetServer
                }

                // 3. Obtain VLESS+Reality session configuration from backend or local subscription store
                val sessionConfig = storage.getServerConfig(targetServer.nodeCode) ?: try {
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

                // 4. Start foreground Android VpnService which builds TUN and launches native Xray-core
                ByMeVpnService.start(
                    context = context,
                    server = targetServer,
                    sessionConfig = sessionConfig
                )

            } catch (e: Exception) {
                Log.e(TAG, "Connect failed: ${e.message}", e)
                ByMeVpnService.stop(context)
                _state.value = VpnConnectionState.ERROR
                _errorMessage.value = e.message ?: (if (com.example.bymevpn.data.LocaleManager.isRussian(context)) "Ошибка подключения" else "Connection error")
            }
        }
    }

    /**
     * Callback from ByMeVpnService when TUN is established and Xray core confirms isRunning == true.
     */
    fun onTunnelConnected(server: ServerNode?) {
        scope.launch(Dispatchers.Main) {
            if (server != null) {
                _currentServer.value = server
            }
            _state.value = VpnConnectionState.CONNECTED
            _errorMessage.value = null
            startTimer()
        }
    }

    /**
     * Callback from ByMeVpnService when tunnel or Xray fails to start.
     */
    fun onTunnelFailed(error: String) {
        scope.launch(Dispatchers.Main) {
            _state.value = VpnConnectionState.ERROR
            _errorMessage.value = error
            stopTimer()
        }
    }

    /**
     * Callback from ByMeVpnService when tunnel is disconnected.
     */
    fun onTunnelDisconnected() {
        scope.launch(Dispatchers.Main) {
            _state.value = VpnConnectionState.DISCONNECTED
            stopTimer()
        }
    }

    /**
     * Re-synchronizes UI state with the background service and Xray core on app launch or screen resume.
     */
    fun syncWithServiceState() {
        val serviceActive = ByMeVpnService.isServiceRunning
        val engineActive = engine.isRunning

        if (serviceActive && engineActive) {
            if (_state.value != VpnConnectionState.CONNECTED) {
                _state.value = VpnConnectionState.CONNECTED
                ByMeVpnService.activeServer?.let { _currentServer.value = it }
                startTimer()
            }
        } else if (!serviceActive && !engineActive && _state.value == VpnConnectionState.CONNECTED) {
            _state.value = VpnConnectionState.DISCONNECTED
            stopTimer()
        }
    }

    fun disconnect() {
        if (_state.value == VpnConnectionState.DISCONNECTED || _state.value == VpnConnectionState.DISCONNECTING) {
            return
        }

        scope.launch {
            _state.value = VpnConnectionState.DISCONNECTING
            try {
                ByMeVpnService.stop(context)
                engine.disconnect()
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
