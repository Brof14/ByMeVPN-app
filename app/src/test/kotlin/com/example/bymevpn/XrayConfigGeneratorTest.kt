package com.example.bymevpn

import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.xray.XrayConfigGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class XrayConfigGeneratorTest {

    @Test
    fun hysteriaUsesXrayOutboundAndTransportFormat() {
        val config = VpnSessionConfig(protocol = "hysteria2", server = "example.org", port = 443, uuid = "secret", encryption = "salamander", flow = "mask", serverName = "example.org", publicKey = "")
        val proxy = XrayConfigGenerator.generateJson(config).getJSONArray("outbounds").getJSONObject(0)
        assertEquals("hysteria", proxy.getString("protocol"))
        assertEquals(2, proxy.getJSONObject("settings").getInt("version"))
        val stream = proxy.getJSONObject("streamSettings")
        assertEquals("hysteria", stream.getString("network"))
        assertEquals("secret", stream.getJSONObject("hysteriaSettings").getString("auth"))
        assertEquals("mask", stream.getJSONObject("finalmask").getJSONArray("udp").getJSONObject(0).getJSONObject("settings").getString("password"))
    }

    @Test
    fun preservesRealitySniFromSubscription() {
        val config = VpnSessionConfig(server = "example.org", port = 443, uuid = "test", security = "reality", serverName = "bymevpn.duckdns.org", publicKey = "test")
        val reality = XrayConfigGenerator.generateJson(config).getJSONArray("outbounds")
            .getJSONObject(0).getJSONObject("streamSettings").getJSONObject("realitySettings")
        assertEquals("bymevpn.duckdns.org", reality.getString("serverName"))
    }

    @Test
    fun russianBypassRulesFollowSetting() {
        val config = VpnSessionConfig(server = "example.org", port = 443, uuid = "test", serverName = "example.org", publicKey = "test")
        val enabled = XrayConfigGenerator.generateJson(config, bypassRussianTraffic = true).getJSONObject("routing").getJSONArray("rules").toString()
        val disabled = XrayConfigGenerator.generateJson(config, bypassRussianTraffic = false).getJSONObject("routing").getJSONArray("rules").toString()
        org.junit.Assert.assertTrue(enabled.contains("geosite:category-ru") && enabled.contains("geoip:ru"))
        org.junit.Assert.assertFalse(disabled.contains("geosite:category-ru") || disabled.contains("geoip:ru"))
    }

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

        // Check inbounds: TUN + SOCKS + HTTP
        val inbounds = json.getJSONArray("inbounds")
        assertEquals(3, inbounds.length())

        val tunInbound = inbounds.getJSONObject(0)
        assertEquals("tun", tunInbound.getString("tag"))
        assertEquals("tun", tunInbound.getString("protocol"))
        assertEquals(1500, tunInbound.getJSONObject("settings").getInt("MTU"))

        val socksInbound = inbounds.getJSONObject(1)
        assertEquals("socks-in", socksInbound.getString("tag"))
        assertEquals(10808, socksInbound.getInt("port"))

        // Check outbounds (proxy, direct, dns-out, block)
        val outbounds = json.getJSONArray("outbounds")
        assertEquals(4, outbounds.length())

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

        // Check stats, policy, and dns
        assertNotNull(json.optJSONObject("stats"))
        assertNotNull(json.optJSONObject("policy"))
        assertNotNull(json.optJSONObject("dns"))
    }
}
