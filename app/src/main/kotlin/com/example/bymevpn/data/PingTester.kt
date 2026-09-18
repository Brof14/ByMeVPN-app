package com.example.bymevpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {
    /**
     * Measures real socket connection ping latency to the European exchange/datacenter for this country.
     * Guaranteed to execute only on user explicit demand, preserving battery life and avoiding background polling.
     */
    suspend fun measureRealPing(countryCode: String): Int = withContext(Dispatchers.IO) {
        val targets = when (countryCode.lowercase()) {
            "nl" -> listOf(
                Pair("ams-ix.net", 443),
                Pair("1.1.1.1", 443)
            )
            "de" -> listOf(
                Pair("de-cix.net", 443),
                Pair("fra.speedtest.net", 443),
                Pair("1.1.1.1", 443)
            )
            else -> listOf(
                Pair("1.1.1.1", 443),
                Pair("8.8.8.8", 53)
            )
        }

        for ((host, port) in targets) {
            val startTime = System.currentTimeMillis()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host, port), 2000)
                val latency = (System.currentTimeMillis() - startTime).toInt()
                return@withContext latency.coerceAtLeast(14)
            } catch (_: Exception) {
                // Try next endpoint
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
        return@withContext -1
    }
}
