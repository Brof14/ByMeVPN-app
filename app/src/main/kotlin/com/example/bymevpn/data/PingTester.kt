package com.example.bymevpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.xray.XrayConfigGenerator
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import libv2ray.Libv2ray
import org.json.JSONArray
import org.json.JSONObject

object PingTester {
    private val probeLimit = Semaphore(3)

    suspend fun measureTunnelDelays(targets: List<Pair<String, VpnSessionConfig>>): Map<String, Int> = coroutineScope {
        targets.map { (nodeCode, config) ->
            async(Dispatchers.IO) {
                probeLimit.withPermit {
                    val delay = try {
                        // Only the subscription's proxy outbound is used; no TUN or local listener is started.
                        val generated = XrayConfigGenerator.generateJson(config)
                        val outbound = generated.getJSONArray("outbounds").getJSONObject(0)
                        val probeConfig = JSONObject().put("outbounds", JSONArray().put(outbound))
                        val measured = Libv2ray.measureOutboundDelay(probeConfig.toString(), "https://www.google.com/generate_204")
                        if (measured < 0) -1 else measured.coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
                    } catch (_: Exception) { -1 }
                    nodeCode to delay
                }
            }
        }.awaitAll().toMap()
    }

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
