package com.example.bymevpn

import com.example.bymevpn.vpn.UnderlyingNetworkPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnderlyingNetworkPolicyTest {
    @Test
    fun onlyPhysicalInternetNetworksCountAsUnderlyingConnectivity() {
        assertTrue(UnderlyingNetworkPolicy.isUsable(hasInternet = true, isVpn = false, hasPhysicalTransport = true))
        assertFalse(UnderlyingNetworkPolicy.isUsable(hasInternet = true, isVpn = true, hasPhysicalTransport = true))
        assertFalse(UnderlyingNetworkPolicy.isUsable(hasInternet = false, isVpn = false, hasPhysicalTransport = true))
        assertFalse(UnderlyingNetworkPolicy.isUsable(hasInternet = true, isVpn = false, hasPhysicalTransport = false))
    }
}
