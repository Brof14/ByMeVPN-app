package com.example.bymevpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {

    /**
     * Measures real socket connection ping latency directly to the server host and port.
     */
    suspend fun measureRealPing(host: String, port: Int = 443, timeoutMs: Int = 1800): Int = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            val latency = (System.currentTimeMillis() - startTime).toInt()
            latency.coerceAtLeast(1)
        } catch (_: Exception) {
            -1
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Concurrently measures ping to multiple servers in parallel.
     * Completes in ~300-500ms regardless of server count.
     */
    suspend fun measureAllConcurrent(
        targets: List<Pair<String, Pair<String, Int>>>,
        timeoutMs: Int = 1800
    ): Map<String, Int> = coroutineScope {
        targets.map { (nodeCode, endpoint) ->
            async(Dispatchers.IO) {
                val ping = measureRealPing(endpoint.first, endpoint.second, timeoutMs)
                nodeCode to ping
            }
        }.awaitAll().toMap()
    }
}
