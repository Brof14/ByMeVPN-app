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
    VpnServer("Germany", "Frankfurt", "DE", "159.69.112.5", 16),
    VpnServer("United States", "New York", "US", "104.244.42.1", 24),
    VpnServer("United Kingdom", "London", "GB", "185.199.108.153", 20),
    VpnServer("Japan", "Tokyo", "JP", "133.130.120.40", 82)
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
