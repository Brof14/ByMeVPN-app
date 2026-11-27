package com.example.bymevpn

import com.example.bymevpn.vpn.NetworkTransport
import com.example.bymevpn.vpn.PhysicalNetworkSnapshot
import com.example.bymevpn.vpn.ReconnectPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectPolicyTest {

    @Test
    fun triggersImmediateProbeOnWifiToCellularHandover() {
        val wifi = PhysicalNetworkSnapshot(true, 101L, NetworkTransport.WIFI)
        val cellular = PhysicalNetworkSnapshot(true, 202L, NetworkTransport.CELLULAR)

        assertTrue(ReconnectPolicy.shouldTriggerImmediateRouteProbe(wifi, cellular))
    }

    @Test
    fun triggersImmediateProbeWhenInternetReturns() {
        val offline = PhysicalNetworkSnapshot(false, null, NetworkTransport.NONE)
        val wifi = PhysicalNetworkSnapshot(true, 101L, NetworkTransport.WIFI)

        assertTrue(ReconnectPolicy.shouldTriggerImmediateRouteProbe(offline, wifi))
    }

    @Test
    fun doesNotTriggerProbeForSameNetworkIdAndTransport() {
        val wifi = PhysicalNetworkSnapshot(true, 101L, NetworkTransport.WIFI)
        val sameWifi = PhysicalNetworkSnapshot(true, 101L, NetworkTransport.WIFI)

        assertFalse(ReconnectPolicy.shouldTriggerImmediateRouteProbe(null, wifi))
        assertFalse(ReconnectPolicy.shouldTriggerImmediateRouteProbe(wifi, sameWifi))
        assertFalse(
            ReconnectPolicy.shouldTriggerImmediateRouteProbe(
                wifi,
                PhysicalNetworkSnapshot(false, null, NetworkTransport.NONE)
            )
        )
    }

    @Test
    fun offlineAttemptDoesNotSleepBlindly() {
        assertEquals(0L, ReconnectPolicy.nextWaitBeforeAttemptMs(0, hasNetwork = false))
        assertEquals(1_500L, ReconnectPolicy.nextWaitBeforeAttemptMs(0, hasNetwork = true))
        assertEquals(20_000L, ReconnectPolicy.nextWaitBeforeAttemptMs(99, hasNetwork = true))
    }
}
