package com.example.bymevpn.vpn

data class VpnServer(
    val country: String,
    val city: String,
    val countryCode: String,
    val ip: String,
    val ping: Int,
    val port: Int = 51820
)

val AVAILABLE_SERVERS = listOf(
    VpnServer("Netherlands", "Amsterdam", "NL", "18.110.221.14", 12),
    VpnServer("Germany", "Frankfurt", "DE", "159.69.112.5", 16)
)

data class VpnSessionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val durationSeconds: Long = 0L,
    val selectedServer: VpnServer = AVAILABLE_SERVERS[0],
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L,
    val errorMessage: String? = null
)
