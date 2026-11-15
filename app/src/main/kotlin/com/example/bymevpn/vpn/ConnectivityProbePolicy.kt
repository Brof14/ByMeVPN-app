package com.example.bymevpn.vpn

/** Remote HTTPS probes are diagnostics, not proof that every app's traffic is protected. */
internal object ConnectivityProbePolicy {
    val targets = listOf("www.cloudflare.com", "www.google.com", "www.microsoft.com")
    const val MAX_CONSECUTIVE_FAILURES = 3

    fun anyTargetReachable(probe: (String) -> Boolean): Boolean = targets.any(probe)

    fun shouldDisconnect(consecutiveFailures: Int): Boolean =
        consecutiveFailures >= MAX_CONSECUTIVE_FAILURES
}
