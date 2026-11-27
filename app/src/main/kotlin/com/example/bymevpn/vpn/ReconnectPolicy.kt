package com.example.bymevpn.vpn

internal enum class NetworkTransport {
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER,
    NONE
}

internal data class PhysicalNetworkSnapshot(
    val hasUsableInternet: Boolean,
    val activeNetworkId: Long?,
    val primaryTransport: NetworkTransport
)

internal object ReconnectPolicy {
    val RETRY_BACKOFF_MS = longArrayOf(1_500L, 3_000L, 6_000L, 12_000L, 20_000L)

    fun shouldTriggerImmediateRouteProbe(
        previous: PhysicalNetworkSnapshot?,
        current: PhysicalNetworkSnapshot
    ): Boolean {
        if (!current.hasUsableInternet || previous == null) return false
        if (!previous.hasUsableInternet) return true
        if (previous.activeNetworkId != null &&
            current.activeNetworkId != null &&
            previous.activeNetworkId != current.activeNetworkId
        ) {
            return true
        }
        return previous.primaryTransport != current.primaryTransport
    }

    fun nextWaitBeforeAttemptMs(attemptIndex: Int, hasNetwork: Boolean): Long {
        if (!hasNetwork) return 0L
        return RETRY_BACKOFF_MS[attemptIndex.coerceIn(0, RETRY_BACKOFF_MS.lastIndex)]
    }
}
