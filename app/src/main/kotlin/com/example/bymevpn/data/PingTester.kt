package com.example.bymevpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {
    /**
     * Measures real socket connection ping latency directly to the server host and port.
     */
    suspend fun measureRealPing(host: String, port: Int = 443): Int = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), 2500)
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
}
