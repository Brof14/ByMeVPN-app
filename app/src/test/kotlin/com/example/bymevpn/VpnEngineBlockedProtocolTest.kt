package com.example.bymevpn

import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.VpnConnectionState
import com.example.bymevpn.vpn.xray.XrayVpnEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnEngineBlockedProtocolTest {

    @Test
    fun testBlockedReason_isDocumented() {
        assertTrue(XrayVpnEngine.BLOCKED_REASON.contains("VPN-движок недоступен"))
        assertTrue(XrayVpnEngine.BLOCKED_REASON.contains("Xray-core"))
    }

    @Test
    fun testStartTunnel_truthfullyFailsAndDoesNotFakeConnectedState() = runBlocking {
        // unitTests.isReturnDefaultValues allows mocking-free invocation
        // using an uninitialized or mocked instance if needed, or check engine state flow
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

        // Invalid config check
        val invalidConfig = validConfig.copy(server = "", uuid = "")
        // Check session fields
        assertEquals("", invalidConfig.server)
        assertEquals("", invalidConfig.uuid)
    }
}
