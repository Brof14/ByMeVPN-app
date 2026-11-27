package com.example.bymevpn.vpn.xray

/**
 * Pure traffic counter tracker for Xray-core (`queryAllOutboundTrafficStats`) with
 * support for both cumulative and reset-on-read Go counter implementations, plus
 * optional kernel UID TrafficStats fallback when Xray outbound counters are silent.
 */
internal class XrayStatsTracker {

    data class ParsedSample(
        val uploadBytes: Long,
        val downloadBytes: Long,
        val hasProxyTag: Boolean
    )

    data class Snapshot(
        val totalBytesIn: Long,
        val totalBytesOut: Long,
        val downloadSpeedBps: Long,
        val uploadSpeedBps: Long
    )

    private var lastSampleTimeMs: Long? = null
    private var prevRawUp: Long = 0L
    private var prevRawDown: Long = 0L
    private var accumulatedUp: Long = 0L
    private var accumulatedDown: Long = 0L
    private var resetOnReadUp: Boolean = false
    private var resetOnReadDown: Boolean = false
    private var hasXraySamples: Boolean = false

    private var uidBaselineRx: Long? = null
    private var uidBaselineTx: Long? = null
    private var prevUidRx: Long = 0L
    private var prevUidTx: Long = 0L
    private var usingUidFallback: Boolean = false

    fun reset(
        initialRawStats: String = "",
        initialUidRxBytes: Long = -1L,
        initialUidTxBytes: Long = -1L,
        nowElapsedMs: Long = 0L
    ) {
        val baseline = parseOutboundStats(initialRawStats)
        lastSampleTimeMs = if (nowElapsedMs > 0L) nowElapsedMs else null
        prevRawUp = baseline.uploadBytes
        prevRawDown = baseline.downloadBytes
        accumulatedUp = 0L
        accumulatedDown = 0L
        resetOnReadUp = false
        resetOnReadDown = false
        hasXraySamples = false

        uidBaselineRx = initialUidRxBytes.takeIf { it >= 0L }
        uidBaselineTx = initialUidTxBytes.takeIf { it >= 0L }
        prevUidRx = uidBaselineRx ?: 0L
        prevUidTx = uidBaselineTx ?: 0L
        usingUidFallback = false
    }

    fun recordSample(
        rawStats: String,
        nowElapsedMs: Long,
        uidRxBytes: Long = -1L,
        uidTxBytes: Long = -1L
    ): Snapshot {
        val prevTime = lastSampleTimeMs
        val elapsedSec = if (prevTime != null) {
            ((nowElapsedMs - prevTime).coerceAtLeast(250L)) / 1000.0
        } else {
            1.0
        }
        lastSampleTimeMs = nowElapsedMs

        val uidRxDelta = computeUidDelta(uidRxBytes, isRx = true)
        val uidTxDelta = computeUidDelta(uidTxBytes, isRx = false)

        val parsed = parseOutboundStats(rawStats)
        val rawDown = parsed.downloadBytes
        val rawUp = parsed.uploadBytes

        if (rawStats.isNotBlank() && (rawDown > 0L || rawUp > 0L || parsed.hasProxyTag)) {
            hasXraySamples = true
            usingUidFallback = false
        }

        if (rawStats.isBlank() && !hasXraySamples && (uidRxDelta > 0L || uidTxDelta > 0L)) {
            usingUidFallback = true
        }

        val deltaDown: Long
        val deltaUp: Long

        if (usingUidFallback && rawStats.isBlank()) {
            deltaDown = uidRxDelta
            deltaUp = uidTxDelta
            accumulatedDown += deltaDown
            accumulatedUp += deltaUp
        } else {
            deltaDown = advanceAxis(
                raw = rawDown,
                prevRaw = prevRawDown,
                isDown = true
            )
            deltaUp = advanceAxis(
                raw = rawUp,
                prevRaw = prevRawUp,
                isDown = false
            )
            prevRawDown = rawDown
            prevRawUp = rawUp
        }

        val speedDown = if (prevTime == null) 0L else (deltaDown / elapsedSec).toLong().coerceAtLeast(0L)
        val speedUp = if (prevTime == null) 0L else (deltaUp / elapsedSec).toLong().coerceAtLeast(0L)

        return Snapshot(
            totalBytesIn = accumulatedDown,
            totalBytesOut = accumulatedUp,
            downloadSpeedBps = speedDown,
            uploadSpeedBps = speedUp
        )
    }

    private fun computeUidDelta(currentUidBytes: Long, isRx: Boolean): Long {
        if (currentUidBytes < 0L) return 0L
        val baseline = if (isRx) uidBaselineRx else uidBaselineTx
        if (baseline == null) {
            if (isRx) {
                uidBaselineRx = currentUidBytes
                prevUidRx = currentUidBytes
            } else {
                uidBaselineTx = currentUidBytes
                prevUidTx = currentUidBytes
            }
            return 0L
        }
        val prev = if (isRx) prevUidRx else prevUidTx
        val delta = (currentUidBytes - prev).coerceAtLeast(0L)
        if (isRx) prevUidRx = currentUidBytes else prevUidTx = currentUidBytes
        return delta
    }

    private fun advanceAxis(
        raw: Long,
        prevRaw: Long,
        isDown: Boolean
    ): Long {
        val isResetMode = if (isDown) resetOnReadDown else resetOnReadUp
        if (isResetMode) {
            val delta = raw.coerceAtLeast(0L)
            if (isDown) accumulatedDown += delta else accumulatedUp += delta
            return delta
        }

        // If a counter drops after being positive, libv2ray resets counters on each read.
        if (raw < prevRaw) {
            if (isDown) resetOnReadDown = true else resetOnReadUp = true
            val delta = raw.coerceAtLeast(0L)
            if (isDown) accumulatedDown += delta else accumulatedUp += delta
            return delta
        }

        val delta = (raw - prevRaw).coerceAtLeast(0L)
        if (isDown) accumulatedDown += delta else accumulatedUp += delta
        return delta
    }

    companion object {
        fun parseOutboundStats(rawStats: String): ParsedSample {
            if (rawStats.isBlank()) return ParsedSample(0L, 0L, hasProxyTag = false)

            var proxyUp = 0L
            var proxyDown = 0L
            var otherUp = 0L
            var otherDown = 0L
            var sawProxyTag = false

            rawStats.split(";").forEach { entry ->
                val trimmed = entry.trim()
                if (trimmed.isEmpty()) return@forEach
                val parts = trimmed.split(",")
                val tag: String
                val direction: String
                val value: Long
                when (parts.size) {
                    3 -> {
                        tag = parts[0].trim().lowercase()
                        direction = parts[1].trim().lowercase()
                        value = parts[2].trim().toLongOrNull()?.coerceAtLeast(0L) ?: return@forEach
                    }
                    2 -> {
                        val key = parts[0].trim().lowercase()
                        tag = key
                        direction = key
                        value = parts[1].trim().toLongOrNull()?.coerceAtLeast(0L) ?: return@forEach
                    }
                    else -> return@forEach
                }

                // Never count direct/bypass, blackhole, or internal DNS loop counters as VPN tunnel traffic.
                if (tag == "block" || tag == "dns-out" || tag == "direct" ||
                    tag.contains(">>>block>>>") || tag.contains(">>>dns-out>>>") || tag.contains(">>>direct>>>")
                ) {
                    return@forEach
                }

                val isProxy = tag == "proxy" || tag.contains(">>>proxy>>>")
                if (isProxy) {
                    sawProxyTag = true
                    when {
                        direction.contains("uplink") -> proxyUp += value
                        direction.contains("downlink") -> proxyDown += value
                    }
                } else {
                    when {
                        direction.contains("uplink") -> otherUp += value
                        direction.contains("downlink") -> otherDown += value
                    }
                }
            }

            return if (sawProxyTag) {
                ParsedSample(uploadBytes = proxyUp, downloadBytes = proxyDown, hasProxyTag = true)
            } else {
                ParsedSample(uploadBytes = otherUp, downloadBytes = otherDown, hasProxyTag = false)
            }
        }
    }
}
