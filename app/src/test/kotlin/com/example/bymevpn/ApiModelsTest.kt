package com.example.bymevpn

import com.example.bymevpn.data.api.ApiConfig
import com.example.bymevpn.data.api.ApiError
import com.example.bymevpn.data.api.ApiJsonParsers
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApiModelsTest {

    @Test
    fun missingServerMetadataDoesNotInventCountryOrLatency() {
        val server = ApiJsonParsers.parseServers(JSONArray("[{\"node_code\":\"node\"}]")).single()
        assertEquals("", server.countryCode)
        assertEquals("", server.city)
        assertEquals(-1, server.pingMs)
        assertEquals(-1, server.loadPercent)
        assertEquals("VPN", server.protocol)
    }

    @Test
    fun unknownRenewalIsNotReportedAsEnabledOrDisabled() {
        assertNull(ApiJsonParsers.parseSubscriptionStatus(JSONObject("{\"status\":\"active\"}")).autoRenew)
        assertEquals(false, ApiJsonParsers.parseSubscriptionStatus(JSONObject("{\"status\":\"active\",\"auto_renew\":false}")).autoRenew)
        assertEquals(true, ApiJsonParsers.parseSubscriptionStatus(JSONObject("{\"status\":\"active\",\"auto_renew\":true}")).autoRenew)
    }

    @Test
    fun trialBootstrapMetadataIsParsedAndMissingFieldsStayDisabled() {
        val bootstrap = ApiJsonParsers.parseSubscriptionStatus(
            JSONObject("""{"status":"trial","max_devices":2,"seconds_remaining":600,"entitlement_bootstrap":true,"entitlement_expires_at":1793670000}""")
        )
        assertEquals(true, bootstrap.entitlementBootstrap)
        assertEquals(1793670000L, bootstrap.entitlementExpiresAt)

        val ordinary = ApiJsonParsers.parseSubscriptionStatus(JSONObject("""{"status":"trial","max_devices":2}"""))
        assertEquals(false, ordinary.entitlementBootstrap)
        assertNull(ordinary.entitlementExpiresAt)
    }

    @Test
    fun deviceParserNeverInventsDeleteIdsAndKeepsDistinctDevicesWithSameModel() {
        val devices = ApiJsonParsers.parseDevices(
            JSONArray(
                """
                [
                  {"device_model":"Xiaomi 2312","platform":"Android","is_current":true},
                  {"id":41,"device_model":"Xiaomi 2312","platform":"Android"},
                  {"id":42,"device_model":"Xiaomi 2312","platform":"Android"}
                ]
                """.trimIndent()
            )
        )

        assertEquals(listOf("41", "42"), devices.map { it.id })
        assertEquals(2, devices.size)
        assertEquals(devices[0].deviceModel, devices[1].deviceModel)
    }

    @Test
    fun duplicateServerRowsMergeOnlyWhenTheirStableIdsMatch() {
        val devices = ApiJsonParsers.parseDevices(
            JSONArray(
                """
                [
                  {"id":41,"device_model":"Xiaomi","last_active":"Yesterday"},
                  {"id":41,"device_model":"Xiaomi","is_current":true,"last_active":"Now"}
                ]
                """.trimIndent()
            )
        )

        assertEquals(1, devices.size)
        assertEquals(true, devices.single().isCurrent)
        assertEquals("Now", devices.single().lastActive)
    }

    @Test
    fun activePanelClientIsNotMistakenForThisPhone() {
        val device = ApiJsonParsers.parseDevices(
            JSONArray("[{\"id\":41,\"device_model\":\"Xiaomi\",\"is_active\":true}]")
        ).single()

        assertEquals(false, device.isCurrent)
    }

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
