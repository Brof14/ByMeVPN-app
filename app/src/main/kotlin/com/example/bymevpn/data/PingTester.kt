package com.example.bymevpn.data

import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.xray.XrayConfigGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import libv2ray.Libv2ray
import org.json.JSONArray
import org.json.JSONObject

object PingTester {
    private val probeUrls = listOf(
        "https://cp.cloudflare.com/generate_204",
        "https://www.google.com/generate_204"
    )

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
                var measured = -1
                for (url in probeUrls) {
                    currentCoroutineContext().ensureActive()
                    measured = Libv2ray.measureOutboundDelay(probeConfig.toString(), url)
                        .takeIf { it > 0 }
                        ?.coerceAtMost(Int.MAX_VALUE.toLong())
                        ?.toInt() ?: -1
                    if (measured > 0) break
                }
                measured
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                -1
            }
            results[nodeCode] = delay
            onMeasured(nodeCode, delay)
        }
        results
    }
}
