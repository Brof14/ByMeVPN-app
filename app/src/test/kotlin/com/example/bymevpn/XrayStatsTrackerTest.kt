package com.example.bymevpn

import com.example.bymevpn.vpn.xray.XrayStatsTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XrayStatsTrackerTest {

    @Test
    fun cumulativeCountersSubtractPreConnectProbeBaseline() {
        val tracker = XrayStatsTracker()
        tracker.reset(
            initialRawStats = "proxy,downlink,1200;proxy,uplink,300;direct,downlink,500",
            initialUidRxBytes = 10_000L,
            initialUidTxBytes = 2_000L,
            nowElapsedMs = 1_000L
        )

        // First post-connect sample after 1s: 3200 total raw down -> only 2000 post-connect bytes
        val s1 = tracker.recordSample(
            rawStats = "proxy,downlink,3200;proxy,uplink,800;direct,downlink,5000",
            nowElapsedMs = 2_000L,
            uidRxBytes = 12_500L,
            uidTxBytes = 2_600L
        )
        assertEquals(2000L, s1.totalBytesIn)
        assertEquals(500L, s1.totalBytesOut)
        assertEquals(2000L, s1.downloadSpeedBps)
        assertEquals(500L, s1.uploadSpeedBps)

        // Growing burst matching UID delta must NOT falsely flip cumulative into reset-on-read (no quadratic blowup)
        val s2 = tracker.recordSample(
            rawStats = "proxy,downlink,13200;proxy,uplink,2800",
            nowElapsedMs = 3_000L,
            uidRxBytes = 22_500L,
            uidTxBytes = 4_600L
        )
        assertEquals(12_000L, s2.totalBytesIn)
        assertEquals(2_500L, s2.totalBytesOut)
        assertEquals(10_000L, s2.downloadSpeedBps)
        assertEquals(2_000L, s2.uploadSpeedBps)
    }

    @Test
    fun resetOnReadCountersAccumulateProperlyWhenCounterDrops() {
        val tracker = XrayStatsTracker()
        tracker.reset(nowElapsedMs = 1_000L)

        val s1 = tracker.recordSample("proxy,downlink,4000;proxy,uplink,1000", nowElapsedMs = 2_000L)
        assertEquals(4000L, s1.totalBytesIn)
        assertEquals(1000L, s1.totalBytesOut)

        // Counter drops (read-and-clear implementation in libv2ray)
        val s2 = tracker.recordSample("proxy,downlink,1500;proxy,uplink,400", nowElapsedMs = 3_000L)
        assertEquals(5500L, s2.totalBytesIn)
        assertEquals(1400L, s2.totalBytesOut)
        assertEquals(1500L, s2.downloadSpeedBps)
        assertEquals(400L, s2.uploadSpeedBps)

        // Subsequent larger delta in reset-on-read mode still adds correctly
        val s3 = tracker.recordSample("proxy,downlink,3000;proxy,uplink,900", nowElapsedMs = 4_000L)
        assertEquals(8500L, s3.totalBytesIn)
        assertEquals(2300L, s3.totalBytesOut)
    }

    @Test
    fun directBlockAndDnsOutboundsAreExcludedFromStats() {
        val parsed = XrayStatsTracker.parseOutboundStats(
            "outbound>>>direct>>>traffic>>>downlink,99999;" +
                "direct,uplink,88888;" +
                "block,downlink,777;" +
                "dns-out,uplink,666;" +
                "outbound>>>proxy>>>traffic>>>downlink,2500;" +
                "outbound>>>proxy>>>traffic>>>uplink,500"
        )
        assertTrue(parsed.hasProxyTag)
        assertEquals(2500L, parsed.downloadBytes)
        assertEquals(500L, parsed.uploadBytes)

        val onlyDirect = XrayStatsTracker.parseOutboundStats(
            "direct,downlink,5000;block,uplink,100;dns-out,downlink,200"
        )
        assertFalse(onlyDirect.hasProxyTag)
        assertEquals(0L, onlyDirect.downloadBytes)
        assertEquals(0L, onlyDirect.uploadBytes)
    }

    @Test
    fun uidFallbackTriggersOnlyWhenRawStatsAreCompletelyBlank() {
        val tracker = XrayStatsTracker()
        tracker.reset(initialUidRxBytes = 1000L, initialUidTxBytes = 500L, nowElapsedMs = 1_000L)

        // Blank rawStats -> uses UID delta
        val s1 = tracker.recordSample(
            rawStats = "",
            nowElapsedMs = 2_000L,
            uidRxBytes = 2500L,
            uidTxBytes = 900L
        )
        assertEquals(1500L, s1.totalBytesIn)
        assertEquals(400L, s1.totalBytesOut)

        // Zero proxy stats string present ("proxy,downlink,0") -> does NOT mix UID fallback traffic
        val s2 = tracker.recordSample(
            rawStats = "proxy,downlink,0;proxy,uplink,0",
            nowElapsedMs = 3_000L,
            uidRxBytes = 9999L,
            uidTxBytes = 9999L
        )
        assertEquals(1500L, s2.totalBytesIn)
        assertEquals(400L, s2.totalBytesOut)
        assertEquals(0L, s2.downloadSpeedBps)
    }
}
