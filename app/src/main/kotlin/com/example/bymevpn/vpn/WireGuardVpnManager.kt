package com.example.bymevpn.vpn

import android.content.Context
import android.util.Log
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.data.settings.SettingsRepository
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.subscription.SubscriptionManager
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Attribute
import com.wireguard.config.Config
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
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
import java.net.InetAddress

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * Official WireGuard-based VPN Manager for ByMeVPN.
 * - Keypair generated strictly on-device using com.wireguard.crypto.KeyPair()
 * - Private key stays strictly in EncryptedSharedPreferences
 * - Config assembled and launched using GoBackend
 * - Real Statistics from backend.getStatistics(tunnel)
 */
class WireGuardVpnManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WireGuardVpnManager"
        private const val TUNNEL_NAME = "ByMeVPN"

        @Volatile
        private var INSTANCE: WireGuardVpnManager? = null

        fun getInstance(context: Context): WireGuardVpnManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WireGuardVpnManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val apiClient = ByMeApiClient(context)
    private val storage = SecureTokenStorage.getInstance(context)
    private val subscriptionManager = SubscriptionManager.getInstance(context)
    private val settingsRepo = SettingsRepository.getInstance(context)

    private var backend: Backend? = null
    private val wireGuardTunnel = object : Tunnel {
        override fun getName(): String = TUNNEL_NAME
        override fun onStateChange(newState: Tunnel.State) {
            Log.i(TAG, "WireGuard Tunnel State changed: $newState")
            when (newState) {
                Tunnel.State.UP -> _state.value = VpnConnectionState.CONNECTED
                Tunnel.State.DOWN -> _state.value = VpnConnectionState.DISCONNECTED
                Tunnel.State.TOGGLE -> { /* no-op transition state */ }
            }
        }
    }

    private val _state = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    private val _currentServer = MutableStateFlow<ServerNode?>(null)
    val currentServer: StateFlow<ServerNode?> = _currentServer.asStateFlow()

    private val _availableServers = MutableStateFlow<List<ServerNode>>(emptyList())
    val availableServers: StateFlow<List<ServerNode>> = _availableServers.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Real stats from WireGuard backend
    private val _bytesIn = MutableStateFlow(0L)
    val bytesIn: StateFlow<Long> = _bytesIn.asStateFlow()

    private val _bytesOut = MutableStateFlow(0L)
    val bytesOut: StateFlow<Long> = _bytesOut.asStateFlow()

    private val _connectedSeconds = MutableStateFlow(0L)
    val connectedSeconds: StateFlow<Long> = _connectedSeconds.asStateFlow()

    private var statsJob: Job? = null
    private var timerJob: Job? = null

    init {
        try {
            backend = GoBackend(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WireGuard GoBackend: ${e.message}", e)
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
        _currentServer.value = server
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Connect to WireGuard VPN session:
     * 1. Check entitlement (72-hour offline rule or online status).
     * 2. Ensure Keypair exists on device or generate a fresh one.
     * 3. Call POST /vpn/session {node_code, public_key}.
     * 4. Build Config with Interface, Peer, AllowedIPs, DNS, MTU and split tunneling.
     * 5. Set state UP with GoBackend.
     */
    fun connect() {
        if (_state.value == VpnConnectionState.CONNECTING || _state.value == VpnConnectionState.CONNECTED) {
            return
        }

        scope.launch {
            _state.value = VpnConnectionState.CONNECTING
            _errorMessage.value = null

            try {
                // 1. Check Entitlement
                if (!subscriptionManager.canConnect()) {
                    // Try refreshing status online once
                    val refreshed = subscriptionManager.refreshStatus()
                    if (refreshed.isFailure || !subscriptionManager.canConnect()) {
                        _state.value = VpnConnectionState.ERROR
                        _errorMessage.value = "SUBSCRIPTION_REQUIRED"
                        return@launch
                    }
                }

                // 2. Ensure KeyPair exists locally
                val keyPair = getOrCreateKeyPair()

                // 3. Select Server Node
                var targetServer = _currentServer.value
                if (targetServer == null) {
                    val servers = apiClient.getServers()
                    _availableServers.value = servers
                    targetServer = servers.firstOrNull() ?: throw IllegalStateException("No servers available")
                    _currentServer.value = targetServer
                }

                // 4. Request VPN Session from Backend
                val sessionConfig = apiClient.createVpnSession(
                    nodeCode = targetServer.nodeCode,
                    publicKey = keyPair.publicKey.toBase64()
                )

                // 5. Read Settings for Split Tunneling & Excluded apps
                val settings = settingsRepo.settingsFlow.first()

                // 6. Build WireGuard Config
                val config = buildWireGuardConfig(keyPair, sessionConfig, settings)

                // 7. Launch Tunnel via GoBackend
                val activeBackend = backend ?: GoBackend(context).also { backend = it }
                activeBackend.setState(wireGuardTunnel, Tunnel.State.UP, config)

                startMetrics()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect WireGuard tunnel: ${e.message}", e)
                _state.value = VpnConnectionState.ERROR
                _errorMessage.value = e.localizedMessage ?: "Connection error"
            }
        }
    }

    fun disconnect() {
        if (_state.value == VpnConnectionState.DISCONNECTED || _state.value == VpnConnectionState.DISCONNECTING) {
            return
        }

        scope.launch {
            _state.value = VpnConnectionState.DISCONNECTING
            stopMetrics()

            try {
                backend?.setState(wireGuardTunnel, Tunnel.State.DOWN, null)
            } catch (e: Exception) {
                Log.w(TAG, "Error bringing down tunnel: ${e.message}")
            }

            try {
                apiClient.deleteVpnSession()
            } catch (e: Exception) {
                Log.w(TAG, "Error notifying backend about disconnect: ${e.message}")
            }

            _state.value = VpnConnectionState.DISCONNECTED
            _connectedSeconds.value = 0L
        }
    }

    private fun getOrCreateKeyPair(): KeyPair {
        val savedPrivate = storage.getWireGuardPrivateKey()
        val savedPublic = storage.getWireGuardPublicKey()

        if (!savedPrivate.isNullOrBlank() && !savedPublic.isNullOrBlank()) {
            return try {
                val priv = Key.fromBase64(savedPrivate)
                KeyPair(priv)
            } catch (e: Exception) {
                generateAndSaveNewKeyPair()
            }
        }
        return generateAndSaveNewKeyPair()
    }

    private fun generateAndSaveNewKeyPair(): KeyPair {
        val kp = KeyPair()
        storage.saveWireGuardKeys(
            privateKey = kp.privateKey.toBase64(),
            publicKey = kp.publicKey.toBase64()
        )
        return kp
    }

    private fun buildWireGuardConfig(
        keyPair: KeyPair,
        session: VpnSessionConfig,
        settings: com.example.bymevpn.data.settings.AppSettings
    ): Config {
        // Interface builder
        val ifaceBuilder = Interface.Builder()
            .setKeyPair(keyPair)
            .addAddress(InetNetwork.parse(session.addressV4))
            .addDnsServer(InetAddress.getByName(session.dns.ifBlank { "1.1.1.1" }))
            .setMtu(if (session.mtu in 1280..1500) session.mtu else 1420)

        // Split Tunneling application rules
        if (settings.splitTunnelingEnabled && settings.excludedApps.isNotEmpty()) {
            ifaceBuilder.excludeApplications(settings.excludedApps)
        }

        // Always exclude ByMeVPN itself to prevent routing loops
        try {
            ifaceBuilder.excludeApplication(context.packageName)
        } catch (e: Exception) {
            // Ignored
        }

        // Peer builder
        val peerBuilder = Peer.Builder()
            .setPublicKey(Key.fromBase64(session.serverPublicKey))
            .setEndpoint(InetEndpoint.parse(session.endpoint))
            .addAllowedIp(InetNetwork.parse(session.allowedIps.ifBlank { "0.0.0.0/0" }))

        return Config.Builder()
            .setInterface(ifaceBuilder.build())
            .addPeer(peerBuilder.build())
            .build()
    }

    private fun startMetrics() {
        statsJob?.cancel()
        timerJob?.cancel()

        statsJob = scope.launch {
            while (isActive && _state.value == VpnConnectionState.CONNECTED) {
                try {
                    val stats: Statistics? = backend?.getStatistics(wireGuardTunnel)
                    if (stats != null) {
                        _bytesIn.value = stats.totalRx()
                        _bytesOut.value = stats.totalTx()
                    }
                } catch (e: Exception) {
                    // Ignore stats reading errors
                }
                delay(1000)
            }
        }

        timerJob = scope.launch {
            while (isActive && _state.value == VpnConnectionState.CONNECTED) {
                delay(1000)
                _connectedSeconds.value += 1
            }
        }
    }

    private fun stopMetrics() {
        statsJob?.cancel()
        timerJob?.cancel()
        statsJob = null
        timerJob = null
    }
}
