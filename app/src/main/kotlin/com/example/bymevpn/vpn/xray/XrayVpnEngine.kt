package com.example.bymevpn.vpn.xray

import android.content.Context
import android.util.Log
import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.VpnConnectionState
import com.example.bymevpn.vpn.VpnEngine
import com.example.bymevpn.vpn.VpnStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of VpnEngine coordinating Xray-core VLESS + Reality tunnel.
 *
 * BLOCKED:
 * реальный Xray-движок недоступен в этой среде.
 * Почему:
 * 1. В среде сборки AI Studio Build репозитории зависимостей ограничены google() и mavenCentral().
 *    Библиотеки Xray/V2Ray для Android (например, com.github.2dust:AndroidLibXrayLite или libv2ray)
 *    не публикуются в Maven Central и требуют подключения JitPack репозитория.
 * 2. В проекте отсутствуют скомпилированные нативные библиотеки Xray-core (.so для arm64-v8a, x86_64)
 *    в app/src/main/jniLibs/ и отсутствует tun2socks-стек (hev-socks5-tunnel / libtun2socks) для перенаправления
 *    IP-пакетов из системного TUN-интерфейса в SOCKS5/VLESS прокси Xray.
 * Где добавить:
 * 1. Настроить репозиторий JitPack в settings.gradle.kts и добавить зависимость Xray AAR (или поместить собранный
 *    AAR с нативными .so в папку app/libs).
 * 2. Интегрировать tun2socks в ByMeVpnService для связки ParcelFileDescriptor TUN с локальным портом Xray (127.0.0.1:10808).
 * 3. Переводить VpnConnectionState.CONNECTED только после подтверждения успешного старта Xray-core и туннелирования трафика.
 */
class XrayVpnEngine private constructor(private val context: Context) : VpnEngine {

    companion object {
        private const val TAG = "XrayVpnEngine"

        const val BLOCKED_REASON = "VPN-движок недоступен в этой сборке (отсутствуют нативные библиотеки Xray-core и tun2socks)"

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

    override val isRunning: Boolean
        get() = _state.value == VpnConnectionState.CONNECTED || _state.value == VpnConnectionState.CONNECTING

    private var activeConfig: VpnSessionConfig? = null

    /**
     * Connects to VLESS + Reality server using provided session configuration.
     * In accordance with the BLOCKED protocol, returns failure because native Xray-core
     * and tun2socks libraries are not compiled into this application build.
     */
    suspend fun startTunnel(config: VpnSessionConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = VpnConnectionState.CONNECTING
            activeConfig = config

            // 1. Validate session config from real backend
            if (config.server.isBlank() || config.uuid.isBlank() || config.publicKey.isBlank()) {
                throw IllegalArgumentException("Invalid VLESS configuration: server, uuid, or publicKey missing")
            }

            // 2. Validate generation of Xray configuration JSON
            val xrayConfigJson = XrayConfigGenerator.generateString(config)
            Log.d(TAG, "Generated Xray VLESS config for ${config.server}:${config.port}")

            // 3. Truthfully report BLOCKED status - DO NOT fake CONNECTED state or traffic stats!
            Log.w(TAG, "BLOCKED: Native Xray-core binaries not bundled in this build environment")
            _state.value = VpnConnectionState.ERROR
            Result.failure(IllegalStateException(BLOCKED_REASON))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Xray tunnel: ${e.message}", e)
            _state.value = VpnConnectionState.ERROR
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        _state.value = VpnConnectionState.DISCONNECTING
        activeConfig = null
        _statistics.value = VpnStatistics()
        _state.value = VpnConnectionState.DISCONNECTED
    }

    override suspend fun reconnect() = withContext(Dispatchers.IO) {
        val config = activeConfig
        if (config != null) {
            disconnect()
            startTunnel(config)
        }
    }
}
