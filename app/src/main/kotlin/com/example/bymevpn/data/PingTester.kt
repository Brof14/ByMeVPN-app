package com.example.bymevpn.data

import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.xray.XrayConfigGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import libv2ray.Libv2ray
import org.json.JSONArray
import org.json.JSONObject

object PingTester {
    private const val MAX_CONCURRENT_PROBES = 4
    private const val PROBE_TIMEOUT_MS = 4_000L
    private val probeUrls = listOf(
        "https://cp.cloudflare.com/generate_204",
        "https://www.google.com/generate_204"
    )

    suspend fun measureTunnelDelays(
        targets: List<Pair<String, VpnSessionConfig>>,
        onChecking: suspend (String) -> Unit = {},
        onMeasured: suspend (String, Int) -> Unit = { _, _ -> }
    ): Map<String, Int> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(MAX_CONCURRENT_PROBES)
        coroutineScope {
            targets.map { (nodeCode, config) ->
                async {
                    semaphore.withPermit {
                        currentCoroutineContext().ensureActive()
                        onChecking(nodeCode)
                        val delay = try {
                            // Probe through the configured proxy outbound; never start a second TUN.
                            val generated = XrayConfigGenerator.generateJson(config)
                            val outbound = generated.getJSONArray("outbounds").getJSONObject(0)
                            val probeConfig = JSONObject().put("outbounds", JSONArray().put(outbound)).toString()
                            withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                                var measured = -1
                                for (url in probeUrls) {
                                    currentCoroutineContext().ensureActive()
                                    measured = Libv2ray.measureOutboundDelay(probeConfig, url)
                                        .takeIf { it > 0 }
                                        ?.coerceAtMost(Int.MAX_VALUE.toLong())
                                        ?.toInt() ?: -1
                                    if (measured > 0) break
                                }
                                measured
                            } ?: -1
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            -1
                        }
                        onMeasured(nodeCode, delay)
                        nodeCode to delay
                    }
                }
            }.awaitAll().toMap()
        }
    }
}
