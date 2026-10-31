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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
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

    /** Human-readable reason why the server list could not be refreshed. */
    private val _serversError = MutableStateFlow<String?>(null)
    val serversError: StateFlow<String?> = _serversError.asStateFlow()

    private val _isLoadingServers = MutableStateFlow(false)
    val isLoadingServers: StateFlow<Boolean> = _isLoadingServers.asStateFlow()

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
    private var reconnectJob: Job? = null
    private var connectionJob: Job? = null
    @Volatile private var connectionRequested = false
    private val serverLoadMutex = Mutex()

    init {
        scope.launch {
            loadServers()
            while (isActive) {
                delay(6L * 60 * 60 * 1000)
                loadServers()
            }
        }
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
            if (cfg != null && !cfg.protocol.equals("hysteria2", true) && !cfg.protocol.equals("hy2", true)) node.nodeCode to Pair(cfg.server, cfg.port) else null
        }

        val pings = if (targets.isNotEmpty()) {
            com.example.bymevpn.data.PingTester.measureAllConcurrent(targets, timeoutMs = 1500)
        } else {
            emptyMap()
        }

        val successfulPings = pings.filterValues { it > 0 }
        val bestNode = if (successfulPings.isNotEmpty()) {
            val realityPinged = servers.filter { it.protocol.contains("reality", ignoreCase = true) && successfulPings.containsKey(it.nodeCode) }
            if (realityPinged.isNotEmpty()) {
                realityPinged.minByOrNull { pings[it.nodeCode] ?: 9999 }
            } else {
                val minEntry = successfulPings.minByOrNull { it.value }
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
            if (cfg != null && cfg.server.isNotBlank() && !cfg.protocol.equals("hysteria2", true) && !cfg.protocol.equals("hy2", true)) {
                node.nodeCode to Pair(cfg.server, cfg.port)
            } else null
        }

        if (targets.isEmpty()) return@withContext emptyMap()
        val pings = com.example.bymevpn.data.PingTester.measureAllConcurrent(targets, timeoutMs = 1500)
        if (pings.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                // The server list may have changed while the probes were in flight.
                val updated = _availableServers.value.map { node ->
                    val p = pings[node.nodeCode]
                    if (p != null) node.copy(pingMs = if (p > 0) p else -2) else node
                }
                _availableServers.value = updated
                val cur = _currentServer.value
                if (cur != null) {
                    val curPing = pings[cur.nodeCode]
                    if (curPing != null) {
                        _currentServer.value = cur.copy(pingMs = if (curPing > 0) curPing else -2)
                    }
                }
            }
        }
        pings
    }

    fun setServers(servers: List<ServerNode>) {
        val sorted = servers.sortedWith(
            compareBy<ServerNode> { if (it.protocol.contains("reality", ignoreCase = true)) 0 else 1 }
                .thenBy { it.pingMs }
        )
        _availableServers.value = sorted
        val preferred = sorted.firstOrNull { storage.getServerConfig(it.nodeCode)?.security == "reality" } ?: sorted.firstOrNull()
        if (_currentServer.value == null || !sorted.any { it.nodeCode == _currentServer.value?.nodeCode }) {
            _currentServer.value = preferred
        }
        scope.launch {
            refreshAllPings()
        }
    }

    suspend fun loadServers(): Result<List<ServerNode>> = serverLoadMutex.withLock {
        _isLoadingServers.value = true
        try {
            loadServersLocked()
        } finally {
            _isLoadingServers.value = false
        }
    }

    private fun serverLoadFailure(message: String, cause: Throwable? = null): Result<List<ServerNode>> {
        _serversError.value = message
        // Keep a previously delivered list: a failed refresh must not erase known servers.
        if (_availableServers.value.isEmpty()) {
            _currentServer.value = null
        }
        return Result.failure(cause ?: IllegalStateException(message))
    }

    private suspend fun loadServersLocked(): Result<List<ServerNode>> = withContext(Dispatchers.IO) {
        try {
            // 1. Show the last saved subscription list immediately, then refresh it in the background.
            val subUrl = storage.getSubscriptionUrl()
            if (!subUrl.isNullOrBlank()) {
                val saved = storage.getServerNodes()
                if (saved.isNotEmpty()) {
                    setServers(saved)
                    _serversError.value = null
                    scope.launch {
                        if (subscriptionManager.refreshStatus().isSuccess) {
                            val refreshed = storage.getServerNodes()
                            if (refreshed.isNotEmpty()) {
                                setServers(refreshed)
                                _serversError.value = null
                            }
                        }
                    }
                    Log.d(TAG, "Serving saved subscription list (${saved.size} nodes)")
                    return@withContext Result.success(saved)
                }
                // Nothing saved yet: refresh in the background so it cannot delay the API request below.
                scope.launch { subscriptionManager.refreshStatus() }
            }

            // API-only accounts also need an offline list, scoped to the signed-in account.
            val ownerId = storage.getCachedUser()?.id.orEmpty()
            val apiCache = storage.getApiServerCache(ownerId)
            if (apiCache != null) {
                setServers(apiCache.nodes)
                _serversError.value = null
                if (subUrl.isNullOrBlank() && isServerCacheFresh(apiCache.savedAt, System.currentTimeMillis())) {
                    Log.d(TAG, "Serving fresh cached API list (${apiCache.nodes.size} nodes)")
                    return@withContext Result.success(apiCache.nodes)
                }
            }

            // 2. Fetch directly from backend API
            val servers = try {
                apiClient.getServers()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Server list request failed: ${e.javaClass.simpleName}")
                emptyList()
            }
            if (ownerId.isNotBlank() && storage.getCachedUser()?.id != ownerId) {
                return@withContext Result.failure(IllegalStateException("Account changed while loading servers"))
            }
            if (servers.isNotEmpty()) {
                if (ownerId.isNotBlank() && storage.getCachedUser()?.id == ownerId) {
                    storage.saveApiServerCache(ownerId, servers, System.currentTimeMillis())
                }
                setServers(servers)
                _serversError.value = null
                Log.d(TAG, "Loaded ${servers.size} servers from the API")
                return@withContext Result.success(servers)
            }

            if (apiCache != null && storage.getCachedUser()?.id == ownerId) {
                Log.d(TAG, "API returned no servers; keeping cached list (${apiCache.nodes.size} nodes)")
                return@withContext Result.success(apiCache.nodes)
            }

            // 3. Fallback to existing valid cached configs in local storage
            val cachedConfigs = storage.getServerConfigs()
            if (cachedConfigs.isNotEmpty()) {
                val savedNodes = storage.getServerNodes().associateBy { it.nodeCode }
                val localNodes = cachedConfigs.map { (code, cfg) ->
                    savedNodes[code] ?: run {
                    val protocol = if (cfg.protocol.equals("hysteria2", true)) "Hysteria 2"
                        else if (cfg.security.equals("reality", true)) "VLESS Reality" else "VLESS ${cfg.security.uppercase()}"
                    ServerNode(
                        nodeCode = code,
                        country = "Сервер",
                        countryCode = "",
                        city = "",
                        flag = "🌐",
                        pingMs = -1,
                        loadPercent = 0,
                        isPremium = false,
                        protocol = protocol,
                        displayName = "Сервер • $protocol"
                    )
                    }
                }
                setServers(localNodes)
                _serversError.value = null
                return@withContext Result.success(localNodes)
            }

            val isRu = com.example.bymevpn.data.LocaleManager.isRussian(context)
            val message = if (isRu)
                "Сервис не вернул список серверов. Повторите попытку позже."
            else "The service returned no servers. Try again a little later."
            serverLoadFailure(message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load servers: ${e.javaClass.simpleName}")
            val isRu = com.example.bymevpn.data.LocaleManager.isRussian(context)
            serverLoadFailure(
                if (isRu) "Не удалось обновить список серверов" else "Could not refresh the server list",
                e
            )
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

        connectionRequested = true
        _state.value = VpnConnectionState.CONNECTING
        _errorMessage.value = null
        connectionJob = scope.launch {

            try {
                // 1. Verify subscription entitlement
                if (!subscriptionManager.canConnect()) {
                    subscriptionManager.refreshStatus()
                    coroutineContext.ensureActive()
                    if (!subscriptionManager.canConnect()) {
                        _state.value = VpnConnectionState.ERROR
                        _errorMessage.value = "SUBSCRIPTION_REQUIRED"
                        return@launch
                    }
                }

                // 2. Ensure server is selected
                var targetServer = _currentServer.value
                if (targetServer == null) {
                    val servers = loadServers().getOrThrow()
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    coroutineContext.ensureActive()
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
                coroutineContext.ensureActive()
                ByMeVpnService.start(
                    context = context,
                    server = targetServer,
                    sessionConfig = sessionConfig
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
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
            if (!connectionRequested) {
                ByMeVpnService.stop(context)
                return@launch
            }
            reconnectJob?.cancel()
            reconnectJob = null
            if (server != null) {
                val checked = server.copy(connectionCheck = 1)
                _currentServer.value = checked
                _availableServers.value = _availableServers.value.map { if (it.nodeCode == server.nodeCode) it.copy(connectionCheck = 1) else it }
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
            if (!connectionRequested) return@launch
            _currentServer.value?.let { failed ->
                _currentServer.value = failed.copy(connectionCheck = -1)
                _availableServers.value = _availableServers.value.map { if (it.nodeCode == failed.nodeCode) it.copy(connectionCheck = -1) else it }
            }
            _state.value = VpnConnectionState.ERROR
            _errorMessage.value = error
            stopTimer()
        }
    }

    /** A live tunnel failed its periodic end-to-end check. Retry briefly when enabled. */
    fun onTunnelConnectivityLost(server: ServerNode?) {
        scope.launch(Dispatchers.Main) {
            if (!connectionRequested || _state.value != VpnConnectionState.CONNECTED) return@launch

            server?.let { _currentServer.value = it }
            stopTimer()
            reconnectJob?.cancel()
            _state.value = VpnConnectionState.CONNECTING
            reconnectJob = scope.launch(Dispatchers.Main) {
                val isRu = com.example.bymevpn.data.LocaleManager.isRussian(context)
                val settings = settingsRepo.settingsFlow.first()
                if (!settings.autoReconnect) {
                    _state.value = VpnConnectionState.ERROR
                    _errorMessage.value = if (isRu)
                        "Связь с VPN потеряна. Проверьте интернет и подключитесь снова."
                    else "VPN connection was lost. Check your internet and reconnect."
                    return@launch
                }
                val retryDelaysSeconds = longArrayOf(2, 4, 8, 16, 30)
                for (waitSeconds in retryDelaysSeconds) {
                    if (!isNetworkAvailable()) {
                        _state.value = VpnConnectionState.CONNECTING
                        _errorMessage.value = if (isRu) "Ждём восстановления интернета…" else "Waiting for internet…"
                    } else {
                        _state.value = VpnConnectionState.CONNECTING
                        _errorMessage.value = if (isRu) "Восстанавливаем VPN-соединение…" else "Reconnecting VPN…"
                    }
                    delay(waitSeconds * 1000)
                    if (_state.value == VpnConnectionState.CONNECTED) return@launch

                    _state.value = VpnConnectionState.DISCONNECTED
                    connect()
                    val outcome = withTimeoutOrNull(90_000) {
                        state.first { it == VpnConnectionState.CONNECTED || it == VpnConnectionState.ERROR }
                    }
                    if (outcome == VpnConnectionState.CONNECTED) return@launch
                    if (outcome == null) {
                        // Never start another tunnel while the previous start is still pending.
                        connectionRequested = false
                        connectionJob?.cancel()
                        ByMeVpnService.stop(context)
                        break
                    }
                }

                if (_state.value != VpnConnectionState.CONNECTED) {
                    _state.value = VpnConnectionState.ERROR
                    _errorMessage.value = if (isRu)
                        "Не удалось восстановить VPN. Проверьте интернет или выберите другой сервер."
                    else "Could not restore the VPN. Check your internet or choose another server."
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Callback from ByMeVpnService when tunnel is disconnected.
     */
    fun onTunnelDisconnected() {
        scope.launch(Dispatchers.Main) {
            if (!connectionRequested && _state.value != VpnConnectionState.ERROR) {
                _state.value = VpnConnectionState.DISCONNECTED
                stopTimer()
            }
        }
    }

    fun onTunnelPermissionRevoked() {
        connectionRequested = false
        connectionJob?.cancel()
        reconnectJob?.cancel()
        scope.launch(Dispatchers.Main) {
            stopTimer()
            _state.value = VpnConnectionState.ERROR
            _errorMessage.value = if (com.example.bymevpn.data.LocaleManager.isRussian(context))
                "Разрешение VPN отозвано. Подключитесь снова и разрешите VPN."
            else "VPN permission was revoked. Reconnect and allow VPN access."
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
        connectionRequested = false
        connectionJob?.cancel()
        connectionJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        if (_state.value == VpnConnectionState.DISCONNECTED || _state.value == VpnConnectionState.DISCONNECTING) {
            return
        }

        scope.launch {
            _state.value = VpnConnectionState.DISCONNECTING
            try {
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
