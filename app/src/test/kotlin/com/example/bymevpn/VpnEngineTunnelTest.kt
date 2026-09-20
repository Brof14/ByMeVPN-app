package com.example.bymevpn

import com.example.bymevpn.data.api.VpnSessionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VpnEngineTunnelTest {

    @Test
    fun testVpnSessionConfig_validParameters() {
        val validConfig = VpnSessionConfig(
            protocol = "vless",
            server = "185.220.101.5",
            port = 443,
            uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            encryption = "none",
            flow = "xtls-rprx-vision",
            security = "reality",
            serverName = "dl.google.com",
            publicKey = "RealPublicKeyFromBackend1234567890abcdef=",
            shortId = "12345678",
            fingerprint = "chrome",
            network = "tcp"
        )

        assertEquals("185.220.101.5", validConfig.server)
        assertEquals(443, validConfig.port)
        assertEquals("vless", validConfig.protocol)
        assertEquals("reality", validConfig.security)
        assertEquals("xtls-rprx-vision", validConfig.flow)
        assertNotNull(validConfig.publicKey)
    }
}
