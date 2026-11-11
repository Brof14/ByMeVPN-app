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
            // Require a user-accepted prominent disclosure even from tiles/boot receivers.
            if (!SecureTokenStorage.getInstance(context).isVpnDisclosureAccepted()) return
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
    private var bootstrapPromotionJob: Job? = null
    private var engineObserverJob: Job? = null
    private var reconnectJob: Job? = null
    private var connectionJob: Job? = null
    private var switchJob: Job? = null
    private var disconnectJob: Job? = null
    @Volatile private var connectionRequested = false
    private val serverLoadMutex = Mutex()
    @Volatile private var automaticSelection = true
    private val pingMutex = Mutex()

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
        automaticSelection = true
        val servers = _availableServers.value
        if (servers.isEmpty()) return@withContext null
        val pings = refreshAllPings()

        val successfulPings = pings.filterValues { it > 0 }
        val bestNode = successfulPings.minByOrNull { it.value }?.let { entry ->
            servers.find { it.nodeCode == entry.key }
        }

        val winner = bestNode ?: servers.firstOrNull()
        winner?.also {
            withContext(Dispatchers.Main) {
                if (automaticSelection) applyServerSelection(it)
            }
        }
    }

    suspend fun refreshAllPings(): Map<String, Int> = withContext(Dispatchers.IO) { pingMutex.withLock {
        val servers = _availableServers.value
        if (servers.isEmpty()) return@withContext emptyMap()

        val storage = SecureTokenStorage.getInstance(context)
        val targets = servers.mapNotNull { node ->
            val cfg = storage.getServerConfig(node.nodeCode)
            if (cfg != null && cfg.server.isNotBlank()) {
                node.nodeCode to cfg
            } else null
        }

        if (targets.isEmpty()) return@withContext emptyMap()
        engine.initCoreEnv()
        val pings = com.example.bymevpn.data.PingTester.measureTunnelDelays(targets)
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
    } }

    fun setServers(servers: List<ServerNode>) {
        val sorted = servers
        _availableServers.value = sorted
        val preferred = sorted.firstOrNull()
        if (_currentServer.value == null || !sorted.any { it.nodeCode == _currentServer.value?.nodeCode }) {
            _currentServer.value = preferred
        }
        scope.launch {
            val pings = refreshAllPings()
            if (automaticSelection && _state.value == VpnConnectionState.DISCONNECTED) {
                val winner = _availableServers.value.filter { (pings[it.nodeCode] ?: -1) > 0 }
                    .minByOrNull { pings[it.nodeCode] ?: Int.MAX_VALUE }
                if (winner != null) applyServerSelection(winner)
            }
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
            var serverRequestError: Throwable? = null
            val servers = try {
                apiClient.getServers()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                serverRequestError = e
                val apiError = e as? com.example.bymevpn.data.api.ApiError
                if (apiError != null) {
                    Log.w(
                        TAG,
                        "Server list request failed (code=${apiError.code}, status=${apiError.httpStatusCode ?: "n/a"}, correlation=${apiError.correlationId ?: "n/a"})"
                    )
                } else {
                    Log.w(TAG, "Server list request failed: ${e.javaClass.simpleName}")
                }
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
            val currentStatus = subscriptionManager.subscriptionStatus.value
            val message = serverRequestError?.let {
                VpnConnectionErrorMapper.subscriptionRequiredMessage(
                    error = it,
                    isRussian = isRu,
                    trialAvailable = currentStatus?.trialAvailable == true,
                    accessActive = currentStatus?.isUsable == true
                ) ?: VpnConnectionErrorMapper.deviceLimitMessage(it, isRu)
            }
                ?: if (isRu)
                    "Серверы не загрузились. Если доступа нет, проверьте лимит устройств: «Аккаунт» → «Мои устройства»."
                else "Servers could not be loaded. If access is unavailable, check your device limit: Account → My devices."
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
        automaticSelection = false
        applyServerSelection(server)
    }

    private fun applyServerSelection(server: ServerNode) {
        val prevServer = _currentServer.value
        _currentServer.value = server

        // Serialize the switch after actual teardown rather than guessing a 500 ms delay.
        if (_state.value in setOf(VpnConnectionState.CONNECTED, VpnConnectionState.CONNECTING) && prevServer?.nodeCode != server.nodeCode) {
            switchJob?.cancel()
            switchJob = scope.launch {
                disconnectInternal()
                disconnectJob?.join()
                if (_currentServer.value?.nodeCode == server.nodeCode) connect()
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun connect() {
        if (_state.value == VpnConnectionState.CONNECTING || _state.value == VpnConnectionState.CONNECTED || _state.value == VpnConnectionState.DISCONNECTING) {
            return
        }

        connectionRequested = true
        _state.value = VpnConnectionState.CONNECTING
        _errorMessage.value = null
        connectionJob = scope.launch {

            try {
                // 1. Verify subscription entitlement
                if (!subscriptionManager.canConnect() ||
                    subscriptionManager.subscriptionStatus.value?.entitlementBootstrap == true
                ) {
                    // Always ask the account API for the authoritative signed snapshot.
                    // A newly installed device may not yet have a device-bound token, so
                    // the normal status refresh alone cannot authorize its first tunnel.
                    subscriptionManager.refreshStatus(forceBackend = true)
                    coroutineContext.ensureActive()
                    if (!subscriptionManager.canConnect() ||
                        subscriptionManager.subscriptionStatus.value?.entitlementBootstrap == true
                    ) {
                        val status = subscriptionManager.subscriptionStatus.value
                        if (status?.isUsable == true) {
                            // /servers is the backend's first-device bootstrap handshake:
                            // it validates the account and panel slot and may issue a
                            // short, device-bound entitlement. The client completes
                            // the HWID subscription fetch before returning these nodes.
                            val bootstrapServers = apiClient.getServers()
                            if (bootstrapServers.isNotEmpty()) setServers(bootstrapServers)
                            coroutineContext.ensureActive()
                            if (!subscriptionManager.canConnect()) {
                                subscriptionManager.refreshStatus(forceBackend = true)
                            }
                        }
                    }
                    coroutineContext.ensureActive()
                    if (!subscriptionManager.canConnect()) {
                        _state.value = VpnConnectionState.ERROR
                        val isRu = com.example.bymevpn.data.LocaleManager.isRussian(context)
                        _errorMessage.value = VpnConnectionErrorMapper.subscriptionRequiredMessage(
                            isRussian = isRu,
                            trialAvailable = subscriptionManager.subscriptionStatus.value?.trialAvailable == true,
                            accessActive = subscriptionManager.subscriptionStatus.value?.isUsable == true
                        )
                        return@launch
                    }
                }

                // 2. Ensure server is selected
                var targetServer = _currentServer.value
                if (targetServer == null) {
                    val servers = loadServers().getOrThrow()
                    targetServer = servers.firstOrNull()
                        ?: throw IllegalStateException(
                            if (com.example.bymevpn.data.LocaleManager.isRussian(context))
                                "Не удалось получить список серверов"
                            else
                                "No servers available"
                        )
                    _currentServer.value = targetServer
                }

                // 3. Refresh actual VPN endpoints before reading a cached session.
                // Existing devices can use configurations returned directly by
                // /servers; only a first-device bootstrap must wait for the feed
                // fetch that registers its HWID with the panel.
                val latestServers = try {
                    val needsPanelRegistration =
                        subscriptionManager.subscriptionStatus.value?.entitlementBootstrap == true
                    apiClient.getServers(awaitSubscriptionRefresh = needsPanelRegistration)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val apiError = e as? com.example.bymevpn.data.api.ApiError
                    val transientApiFailure = apiError?.code in setOf(
                        "NETWORK_ERROR", "HTTP_502", "HTTP_503", "HTTP_504"
                    )
                    if (!transientApiFailure && e !is java.io.IOException) throw e
                    Log.w(TAG, "Endpoint refresh unavailable; using time-limited offline configuration")
                    emptyList()
                }
                coroutineContext.ensureActive()
                if (latestServers.isNotEmpty()) {
                    val preferredCode = targetServer.nodeCode
                    targetServer = latestServers.firstOrNull { it.nodeCode == preferredCode }
                        ?: latestServers.first()
                    setServers(latestServers)
                    _currentServer.value = targetServer
                }

                // 4. Use the freshly imported subscription/API session (if present);
                // otherwise ask for a session. Never manufacture a VPN endpoint.
                val sessionConfig = storage.getServerConfig(targetServer.nodeCode) ?: try {
                    // Refresh expired or legacy endpoint data before connecting. This
                    // lets DNS/backend changes take effect without keeping old IPs forever.
                    subscriptionManager.refreshStatus()
                    coroutineContext.ensureActive()
                    storage.getServerConfig(targetServer.nodeCode)
                        ?: apiClient.createVpnSession(targetServer.nodeCode)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    coroutineContext.ensureActive()
                    val isRu = com.example.bymevpn.data.LocaleManager.isRussian(context)
                    val rawMsg = e.message ?: ""
                    val deviceLimitMessage = VpnConnectionErrorMapper.deviceLimitMessage(e, isRu)
                    val subscriptionRequiredMessage = VpnConnectionErrorMapper.subscriptionRequiredMessage(
                        e,
                        isRu,
                        subscriptionManager.subscriptionStatus.value?.trialAvailable == true,
                        subscriptionManager.subscriptionStatus.value?.isUsable == true
                    )
                    val msg = when {
                        deviceLimitMessage != null -> deviceLimitMessage
                        subscriptionRequiredMessage != null -> subscriptionRequiredMessage
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

                // 5. Start foreground Android VpnService which builds TUN and launches native Xray-core
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
                Log.e(TAG, "VPN connection failed: ${e.javaClass.simpleName}")
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
            if (subscriptionManager.subscriptionStatus.value?.entitlementBootstrap == true) {
                promoteDeviceBootstrapAfterConnection()
            }
        }
    }

    /**
     * The backend grants a short bootstrap entitlement so a new device can make
     * its first tunnel connection and register its HWID with the panel. Once the
     * panel sees that device, immediately exchange the bootstrap snapshot for the
     * ordinary subscription entitlement. Never leave the tunnel running if confirmation
     * does not arrive during the bounded promotion window.
     */
    private fun promoteDeviceBootstrapAfterConnection() {
        bootstrapPromotionJob?.cancel()
        bootstrapPromotionJob = scope.launch {
            val retryDelaysMs = longArrayOf(0L, 1_500L, 3_000L, 6_000L, 10_000L, 15_000L)
            var promoted = false
            for (delayMs in retryDelaysMs) {
                if (delayMs > 0) delay(delayMs)
                if (!connectionRequested || _state.value != VpnConnectionState.CONNECTED) return@launch

                val refreshed = subscriptionManager.refreshStatus(forceBackend = true).getOrNull()
                if (refreshed != null && !refreshed.entitlementBootstrap && subscriptionManager.canConnect()) {
                    promoted = true
                    break
                }
            }

            if (!promoted && connectionRequested && _state.value == VpnConnectionState.CONNECTED) {
                Log.w(TAG, "Device was not confirmed by the backend after bootstrap connection")
                connectionRequested = false
                bootstrapPromotionJob = null
                ByMeVpnService.stop(context)
                stopTimer()
                _state.value = VpnConnectionState.ERROR
                _errorMessage.value = if (com.example.bymevpn.data.LocaleManager.isRussian(context))
                    "Не удалось подтвердить устройство. Попробуйте подключиться ещё раз."
                else
                    "Could not confirm this device. Please try connecting again."
            } else {
                bootstrapPromotionJob = null
                if (promoted) loadServers()
            }
        }
    }

    /**
     * Callback from ByMeVpnService when tunnel or Xray fails to start.
     */
    fun onTunnelFailed(error: String) {
        scope.launch(Dispatchers.Main) {
            bootstrapPromotionJob?.cancel()
            bootstrapPromotionJob = null
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
            bootstrapPromotionJob?.cancel()
            bootstrapPromotionJob = null
            if (!connectionRequested && _state.value != VpnConnectionState.ERROR) {
                _state.value = VpnConnectionState.DISCONNECTED
                stopTimer()
            }
        }
    }

    fun onTunnelPermissionRevoked() {
        connectionRequested = false
        bootstrapPromotionJob?.cancel()
        bootstrapPromotionJob = null
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
        switchJob?.cancel()
        switchJob = null
        disconnectInternal()
    }

    private fun disconnectInternal() {
        connectionRequested = false
        bootstrapPromotionJob?.cancel()
        bootstrapPromotionJob = null
        connectionJob?.cancel()
        connectionJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        if (_state.value == VpnConnectionState.DISCONNECTING) return
        if (_state.value == VpnConnectionState.DISCONNECTED && !ByMeVpnService.isServiceRunning && !engine.isRunning) return

        _state.value = VpnConnectionState.DISCONNECTING
        disconnectJob = scope.launch {
            try {
                ByMeVpnService.stop(context)
                val stopped = withTimeoutOrNull(15_000L) {
                    while (ByMeVpnService.isServiceRunning || engine.isRunning) delay(50L)
                    true
                } ?: false
                if (!stopped) Log.w(TAG, "VPN tunnel did not report teardown within timeout")
                apiClient.deleteVpnSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "VPN disconnect cleanup failed: ${e.javaClass.simpleName}")
            } finally {
                if (!connectionRequested) _state.value = VpnConnectionState.DISCONNECTED
                stopTimer()
            }
        }
    }

    fun reconnect() {
        switchJob?.cancel()
        switchJob = scope.launch {
            disconnectInternal()
            disconnectJob?.join()
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
