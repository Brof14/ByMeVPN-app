package com.example.bymevpn

import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.vpn.xray.XrayConfigGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsModePolicyTest {

    private val baseConfig = VpnSessionConfig(
        server = "194.87.12.34",
        port = 443,
        uuid = "test-uuid-1234",
        security = "reality",
        serverName = "cloudflare.com",
        publicKey = "dGVzdC1wdWJsaWMta2V5LXRlc3QtcHVibGljLWtleS0x="
    )

    @Test
    fun cloudflareDnsModeGeneratesCorrectServers() {
        val json = XrayConfigGenerator.generateJson(baseConfig, dnsMode = "cloudflare")
        val dns = json.getJSONObject("dns")
        val servers = dns.getJSONArray("servers")

        assertEquals(3, servers.length())
        assertEquals("1.1.1.1", servers.getString(0))
        assertEquals("8.8.8.8", servers.getString(1))
        assertEquals("77.88.8.8", servers.getString(2))
        assertEquals("UseIPv4", dns.getString("queryStrategy"))
    }

    @Test
    fun adguardDnsModeGeneratesAdGuardServers() {
        val json = XrayConfigGenerator.generateJson(baseConfig, dnsMode = "adguard")
        val dns = json.getJSONObject("dns")
        val servers = dns.getJSONArray("servers")

        assertEquals(2, servers.length())
        assertEquals("94.140.14.14", servers.getString(0))
        assertEquals("94.140.15.15", servers.getString(1))
        assertEquals("UseIPv4", dns.getString("queryStrategy"))
    }

    @Test
    fun googleDnsModeGeneratesGoogleServers() {
        val json = XrayConfigGenerator.generateJson(baseConfig, dnsMode = "google")
        val dns = json.getJSONObject("dns")
        val servers = dns.getJSONArray("servers")

        assertEquals(2, servers.length())
        assertEquals("8.8.8.8", servers.getString(0))
        assertEquals("8.8.4.4", servers.getString(1))
        assertEquals("UseIPv4", dns.getString("queryStrategy"))
    }

    @Test
    fun fallbackDnsModeDefaultsToCloudflare() {
        val json = XrayConfigGenerator.generateJson(baseConfig, dnsMode = "custom_unknown")
        val dns = json.getJSONObject("dns")
        val servers = dns.getJSONArray("servers")

        assertEquals(3, servers.length())
        assertEquals("1.1.1.1", servers.getString(0))
    }

    @Test
    fun tunInboundIncludesSniffingAndMtu() {
        val json = XrayConfigGenerator.generateJson(baseConfig, dnsMode = "cloudflare")
        val inbounds = json.getJSONArray("inbounds")
        val tun = inbounds.getJSONObject(0)

        assertEquals("tun", tun.getString("tag"))
        val settings = tun.getJSONObject("settings")
        assertEquals(1500, settings.getInt("mtu"))
        assertEquals(1500, settings.getInt("MTU"))

        val sniffing = tun.getJSONObject("sniffing")
        assertTrue(sniffing.getBoolean("enabled"))
        assertTrue(sniffing.getBoolean("routeOnly"))
    }
}
