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
import kotlinx.coroutines.withContext
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import libv2ray.Libv2ray
import org.json.JSONArray
import org.json.JSONObject

object PingTester {
    internal const val MAX_CONCURRENT_PROBES = 4
    private const val PROBE_TIMEOUT_MS = 4_000L
    private val nativeProbeExecutor = ThreadPoolExecutor(
        MAX_CONCURRENT_PROBES,
        MAX_CONCURRENT_PROBES,
        0L,
        TimeUnit.MILLISECONDS,
        SynchronousQueue(),
        { task -> Thread(task, "bymevpn-ping").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy()
    )
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
                            val future: Future<Int> = nativeProbeExecutor.submit<Int> {
                                val measurements = probeUrls.mapNotNull { url ->
                                    Libv2ray.measureOutboundDelay(probeConfig, url)
                                        .takeIf { it > 0 }
                                        ?.coerceAtMost(Int.MAX_VALUE.toLong())
                                        ?.toInt()
                                }.sorted()
                                when (measurements.size) {
                                    0 -> -1
                                    1 -> measurements[0]
                                    else -> ((measurements[0].toLong() + measurements[1]) / 2L).toInt()
                                }
                            }
                            try {
                                future.get(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                            } catch (_: java.util.concurrent.TimeoutException) {
                                future.cancel(true)
                                -1
                            } catch (_: java.util.concurrent.RejectedExecutionException) {
                                -1
                            }
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
