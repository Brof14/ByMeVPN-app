package com.example.bymevpn.vpn

import com.example.bymevpn.data.api.ServerNode

internal object PingSelectionPolicy {
    const val SUCCESS_CACHE_TTL_MS = 10 * 60 * 1000L
    const val FAILURE_CACHE_TTL_MS = 30 * 1000L

    fun shouldUseCachedPing(pingMs: Int, ageMs: Long): Boolean {
        if (ageMs < 0L) return false
        return if (pingMs > 0) {
            ageMs < SUCCESS_CACHE_TTL_MS
        } else {
            ageMs < FAILURE_CACHE_TTL_MS
        }
    }

    fun aggregateLatencySamples(samples: List<Int>): Int {
        val valid = samples.filter { it > 0 }.sorted()
        return when (valid.size) {
            0 -> -1
            1 -> valid[0]
            else -> {
                // Only average the second endpoint when it tracks the best probe closely;
                // a single throttled CDN endpoint should not penalize an otherwise fast node.
                if (valid[1] <= valid[0] * 2 + 40) {
                    ((valid[0].toLong() + valid[1]) / 2L).toInt()
                } else {
                    valid[0]
                }
            }
        }
    }

    fun pickBestServer(
        servers: List<ServerNode>,
        measuredPings: Map<String, Int> = emptyMap()
    ): ServerNode? {
        if (servers.isEmpty()) return null

        fun effectivePing(node: ServerNode): Int =
            measuredPings[node.nodeCode] ?: node.pingMs

        fun protocolPriority(node: ServerNode): Int {
            val proto = node.protocol.lowercase()
            return when {
                proto.contains("reality") -> 0
                proto.contains("xhttp") || proto.contains("splithttp") -> 1
                proto.contains("hysteria") -> 2
                else -> 3
            }
        }

        val comparator = compareBy<ServerNode>(
            { effectivePing(it) },
            { it.loadPercent.coerceAtLeast(0) },
            { protocolPriority(it) }
        )

        val healthyMeasured = servers
            .filter { it.connectionCheck >= 0 && effectivePing(it) > 0 }
            .minWithOrNull(comparator)
        if (healthyMeasured != null) return healthyMeasured

        val anyMeasured = servers
            .filter { effectivePing(it) > 0 }
            .minWithOrNull(comparator)
        if (anyMeasured != null) return anyMeasured

        return servers.firstOrNull { it.connectionCheck >= 0 } ?: servers.firstOrNull()
    }
}
