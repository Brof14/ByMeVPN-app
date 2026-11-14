package com.example.bymevpn

import com.example.bymevpn.data.api.ApiJsonParsers
import com.example.bymevpn.data.api.VpnSessionConfig
import com.example.bymevpn.data.subscription.XraySubscriptionParser
import com.example.bymevpn.vpn.xray.XrayConfigGenerator
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class VpnConfigRegressionTest {
    private val id = "11111111-2222-3333-4444-555555555555"

    @Test
    fun vlessUriPreservesAddressRealityAndTransportSettings() {
        val uri = "vless://$id@vpn.example.org:16287?security=reality&pbk=public-test" +
            "&sid=abcd1234&sni=front.example.org&fp=chrome&flow=xtls-rprx-vision&type=grpc" +
            "&serviceName=my%2Bservice&mode=multi#Frankfurt%20node"
        val parsed = XraySubscriptionParser.parse(uri)
        val config = parsed.configs.getValue(parsed.servers.single().nodeCode)
        assertEquals("vpn.example.org", config.server)
        assertEquals(16287, config.port)
        assertEquals("reality", config.security)
        assertEquals("front.example.org", config.serverName)
        assertEquals("public-test", config.publicKey)
        assertEquals("abcd1234", config.shortId)
        assertEquals("chrome", config.fingerprint)
        assertEquals("xtls-rprx-vision", config.flow)
        assertEquals("my+service", config.transportPath)
        val proxy = XrayConfigGenerator.generateJson(config).getJSONArray("outbounds").getJSONObject(0)
        val stream = proxy.getJSONObject("streamSettings")
        assertEquals("my+service", stream.getJSONObject("grpcSettings").getString("serviceName"))
        assertTrue(stream.getJSONObject("grpcSettings").getBoolean("multiMode"))
        assertEquals("front.example.org", stream.getJSONObject("realitySettings").getString("serverName"))
        assertEquals(16287, proxy.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0).getInt("port"))
    }

    @Test
    fun ipv6HostAndEscapedHysteriaPasswordArePreserved() {
        val uri = "hy2://p%40ss%2Bword@[2001:db8::5]:59763?sni=hy.example.org" +
            "&obfs=salamander&obfs-password=mask%2Bvalue#IPv6%20node"
        val parsed = XraySubscriptionParser.parse(uri)
        val config = parsed.configs.getValue(parsed.servers.single().nodeCode)
        assertEquals("2001:db8::5", config.server)
        assertEquals(59763, config.port)
        assertEquals("p@ss+word", config.uuid)
        assertEquals("mask+value", config.flow)
        val proxy = XrayConfigGenerator.generateJson(config).getJSONArray("outbounds").getJSONObject(0)
        assertEquals("hysteria", proxy.getString("protocol"))
        assertEquals("2001:db8::5", proxy.getJSONObject("settings").getString("address"))
        assertEquals(59763, proxy.getJSONObject("settings").getInt("port"))
    }

    @Test
    fun vlessIpv6EndpointPreservesPortInsteadOfSplittingColons() {
        val uri = "vless://$id@[2001:db8::9]:54662?security=reality&pbk=testkey&sni=edge.example.org"
        val parsed = XraySubscriptionParser.parse(uri)
        val cfg = parsed.configs.getValue(parsed.servers.single().nodeCode)
        assertEquals("2001:db8::9", cfg.server)
        assertEquals(54662, cfg.port)
        val endpoint = XrayConfigGenerator.generateJson(cfg).getJSONArray("outbounds")
            .getJSONObject(0).getJSONObject("settings").getJSONArray("vnext").getJSONObject(0)
        assertEquals("2001:db8::9", endpoint.getString("address"))
        assertEquals(54662, endpoint.getInt("port"))
    }

    @Test
    fun base64Hy2OnlyFeedDecodes() {
        val uri = "hy2://password@example.org:59763?sni=example.org#hy2"
        val encoded = Base64.getEncoder().encodeToString(uri.toByteArray(Charsets.UTF_8))
        assertEquals("hysteria2", XraySubscriptionParser.parse(encoded).configs.values.single().protocol)
    }

    @Test
    fun missingInvalidPortsAndUnsupportedUrisNeverBecomePort443() {
        for (uri in listOf(
            "vless://$id@example.org?security=reality&pbk=test",
            "vless://$id@example.org:99999?security=reality&pbk=test",
            "vless://$id@example.org:abc?security=reality&pbk=test",
            "hy2://password@example.org:0",
            "vmess://unsupported@example.org:443"
        )) {
            assertTrue("Unexpected accepted URI: $uri", XraySubscriptionParser.parse(uri).servers.isEmpty())
        }
    }

    @Test
    fun missingApiPortFailsBeforeXrayInsteadOfAssumingHttpsPort() {
        val config = ApiJsonParsers.parseVpnSessionConfig(JSONObject(
            """{"protocol":"vless","server":"vpn.example.org","uuid":"$id","security":"reality","server_name":"front.example.org","public_key":"key"}"""
        ))
        assertEquals(0, config.port)
        assertThrows(IllegalArgumentException::class.java) { XrayConfigGenerator.generateJson(config) }
    }

    @Test
    fun tlsDoesNotInheritBackendHostnameAsSni() {
        val config = VpnSessionConfig(protocol = "vless", server = "vpn.example.org", port = 54662,
            uuid = id, flow = "", security = "tls", serverName = "", publicKey = "")
        val stream = XrayConfigGenerator.generateJson(config).getJSONArray("outbounds")
            .getJSONObject(0).getJSONObject("streamSettings")
        assertFalse(stream.getJSONObject("tlsSettings").has("serverName"))
    }

    @Test
    fun unknownProtocolAndUnsupportedTransportsAreRejected() {
        val valid = VpnSessionConfig(protocol = "vless", server = "vpn.example.org", port = 16287,
            uuid = id, security = "reality", serverName = "front.example.org", publicKey = "key")
        assertThrows(IllegalArgumentException::class.java) {
            XrayConfigGenerator.generateJson(valid.copy(protocol = "vmess"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            XrayConfigGenerator.generateJson(valid.copy(network = "quic"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            XrayConfigGenerator.generateJson(valid.copy(publicKey = ""))
        }
    }

    @Test
    fun tlsAlpnFromSubscriptionIsPreservedWithoutInventedEntries() {
        val uri = "vless://$id@vpn.example.org:8443?security=tls&sni=vpn.example.org&alpn=h2%2Chttp%2F1.1"
        val cfg = XraySubscriptionParser.parse(uri).configs.values.single()
        val stream = XrayConfigGenerator.generateJson(cfg).getJSONArray("outbounds")
            .getJSONObject(0).getJSONObject("streamSettings")
        val alpn = stream.getJSONObject("tlsSettings").getJSONArray("alpn")
        assertEquals("h2", alpn.getString(0))
        assertEquals("http/1.1", alpn.getString(1))
        assertEquals(2, alpn.length())
        val api = ApiJsonParsers.parseVpnSessionConfig(JSONObject(
            """{"protocol":"vless","server":"vpn.example.org","port":8443,"uuid":"$id","security":"tls","server_name":"vpn.example.org","alpn":["h2"]}"""
        ))
        assertEquals("h2", api.alpn)
    }

    @Test
    fun websocketAndHttpUpgradeDoNotLosePathOrHost() {
        for (network in listOf("ws", "httpupgrade")) {
            val config = VpnSessionConfig(protocol = "vless", server = "vpn.example.org", port = 8443,
                uuid = id, flow = "", security = "tls", serverName = "vpn.example.org", publicKey = "",
                network = network, transportPath = "/route", transportHost = "edge.example.org")
            val stream = XrayConfigGenerator.generateJson(config).getJSONArray("outbounds")
                .getJSONObject(0).getJSONObject("streamSettings")
            val settings = if (network == "ws") stream.getJSONObject("wsSettings")
                           else stream.getJSONObject("httpupgradeSettings")
            assertEquals("/route", settings.getString("path"))
            assertEquals("edge.example.org", if (network == "ws")
                settings.getJSONObject("headers").getString("Host") else settings.getString("host"))
        }
    }

    @Test
    fun grpcSessionParsesBackendSnakeCaseServiceName() {
        val config = ApiJsonParsers.parseVpnSessionConfig(JSONObject(
            """{"protocol":"vless","server":"vpn.example.org","port":54662,"uuid":"$id","security":"reality","server_name":"front.example.org","public_key":"key","network":"grpc","transport_path":"vpn.Service/Connect"}"""
        ))

        assertEquals("grpc", config.network)
        assertEquals("vpn.Service/Connect", config.transportPath)

        val stream = XrayConfigGenerator.generateJson(config).getJSONArray("outbounds")
            .getJSONObject(0).getJSONObject("streamSettings")
        assertEquals("vpn.Service/Connect", stream.getJSONObject("grpcSettings").getString("serviceName"))
    }
}
