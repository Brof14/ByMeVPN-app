package com.example.bymevpn.vpn

import kotlinx.coroutines.flow.StateFlow

/**
 * High-level connection states for ByMeVPN tunnel.
 */
enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * Real-time traffic statistics from VPN engine.
 */
data class VpnStatistics(
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L
)

/**
 * Abstract VPN Engine interface decoupling UI and service layers from concrete tunnel backends.
 * Concrete implementation will be XrayVpnEngine (VLESS + Reality).
 */
interface VpnEngine {
    val state: StateFlow<VpnConnectionState>
    val statistics: StateFlow<VpnStatistics>
    suspend fun disconnect()
    suspend fun reconnect()
    val isRunning: Boolean
}
