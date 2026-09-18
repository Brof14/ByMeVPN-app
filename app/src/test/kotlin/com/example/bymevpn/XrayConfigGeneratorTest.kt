package com.example.bymevpn

import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.xray.XrayConfigGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class XrayConfigGeneratorTest {

    @Test
    fun testGenerateJson_structure() {
        val config = VpnSessionConfig(
            protocol = "vless",
            server = "194.87.12.34",
            port = 443,
            uuid = "11111111-2222-3333-4444-555555555555",
            encryption = "none",
            flow = "xtls-rprx-vision",
            security = "reality",
            serverName = "gateway.cloudflare.com",
            publicKey = "0123456789abcdef0123456789abcdef0123456789a=",
            shortId = "123456",
            fingerprint = "chrome",
            network = "tcp"
        )

        val json = XrayConfigGenerator.generateJson(config)

        // Check inbounds
        val inbounds = json.getJSONArray("inbounds")
        assertEquals(2, inbounds.length())
        assertEquals("socks-in", inbounds.getJSONObject(0).getString("tag"))
        assertEquals(10808, inbounds.getJSONObject(0).getInt("port"))

        // Check outbounds
        val outbounds = json.getJSONArray("outbounds")
        assertEquals(3, outbounds.length())

        val proxy = outbounds.getJSONObject(0)
        assertEquals("vless", proxy.getString("protocol"))
        assertEquals("proxy", proxy.getString("tag"))

        val vnext = proxy.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0)
        assertEquals("194.87.12.34", vnext.getString("address"))
        assertEquals(443, vnext.getInt("port"))

        val user = vnext.getJSONArray("users").getJSONObject(0)
        assertEquals("11111111-2222-3333-4444-555555555555", user.getString("id"))
        assertEquals("xtls-rprx-vision", user.getString("flow"))

        val stream = proxy.getJSONObject("streamSettings")
        assertEquals("tcp", stream.getString("network"))
        assertEquals("reality", stream.getString("security"))

        val reality = stream.getJSONObject("realitySettings")
        assertEquals("gateway.cloudflare.com", reality.getString("serverName"))
        assertEquals("0123456789abcdef0123456789abcdef0123456789a=", reality.getString("publicKey"))
        assertEquals("123456", reality.getString("shortId"))
        assertEquals("chrome", reality.getString("fingerprint"))
    }
}
