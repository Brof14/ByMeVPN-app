package com.example.bymevpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.net.InetSocketAddress
import java.net.Socket
import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.xray.XrayConfigGenerator
import libv2ray.Libv2ray
import org.json.JSONArray
import org.json.JSONObject

object PingTester {
    private const val PROBE_URL = "https://cp.cloudflare.com/generate_204"

    suspend fun measureTunnelDelays(
        targets: List<Pair<String, VpnSessionConfig>>,
        onMeasured: suspend (String, Int) -> Unit = { _, _ -> }
    ): Map<String, Int> = withContext(Dispatchers.IO) {
        val results = linkedMapOf<String, Int>()
        for ((nodeCode, config) in targets) {
            currentCoroutineContext().ensureActive()
            val delay = try {
                // Probe through the configured proxy outbound; never start a second TUN.
                val generated = XrayConfigGenerator.generateJson(config)
                val outbound = generated.getJSONArray("outbounds").getJSONObject(0)
                val probeConfig = JSONObject().put("outbounds", JSONArray().put(outbound))
                Libv2ray.measureOutboundDelay(probeConfig.toString(), PROBE_URL)
                    .takeIf { it > 0 }
                    ?.coerceAtMost(Int.MAX_VALUE.toLong())
                    ?.toInt() ?: -1
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { -1 }
            results[nodeCode] = delay
            onMeasured(nodeCode, delay)
        }
        results
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
