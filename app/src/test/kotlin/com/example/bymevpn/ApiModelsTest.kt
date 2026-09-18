package com.example.bymevpn

import com.example.bymevpn.data.api.ApiConfig
import com.example.bymevpn.data.api.ApiError
import com.example.bymevpn.data.api.ApiJsonParsers
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApiModelsTest {

    @Test
    fun testParseVpnSessionConfig_vlessReality() {
        val jsonString = """
            {
              "protocol": "vless",
              "server": "185.123.45.67",
              "port": 443,
              "uuid": "b7f12345-6789-abcd-ef01-23456789abcd",
              "encryption": "none",
              "flow": "xtls-rprx-vision",
              "security": "reality",
              "server_name": "example.com",
              "public_key": "xrayRealityPublicKeyPlaceholder123456789=",
              "short_id": "abcd1234",
              "fingerprint": "chrome",
              "network": "tcp",
              "expires_at": "2026-09-18T12:00:00Z"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val config = ApiJsonParsers.parseVpnSessionConfig(json)

        assertEquals("vless", config.protocol)
        assertEquals("185.123.45.67", config.server)
        assertEquals(443, config.port)
        assertEquals("b7f12345-6789-abcd-ef01-23456789abcd", config.uuid)
        assertEquals("none", config.encryption)
        assertEquals("xtls-rprx-vision", config.flow)
        assertEquals("reality", config.security)
        assertEquals("example.com", config.serverName)
        assertEquals("xrayRealityPublicKeyPlaceholder123456789=", config.publicKey)
        assertEquals("abcd1234", config.shortId)
        assertEquals("chrome", config.fingerprint)
        assertEquals("tcp", config.network)
        assertEquals("2026-09-18T12:00:00Z", config.expiresAt)
    }

    @Test
    fun testApiError_structuredError() {
        val jsonString = """
            {
              "error": {
                "code": "SUBSCRIPTION_REQUIRED",
                "message": "Active subscription required"
              }
            }
        """.trimIndent()

        val error = ApiError.fromJson(jsonString)
        assertEquals("SUBSCRIPTION_REQUIRED", error.code)
        assertEquals("Active subscription required", error.message)
    }

    @Test
    fun testApiError_stringError() {
        val jsonString = """{"error": "Invalid credentials"}"""
        val error = ApiError.fromJson(jsonString)
        assertEquals("API_ERROR", error.code)
        assertEquals("Invalid credentials", error.message)
    }
}
