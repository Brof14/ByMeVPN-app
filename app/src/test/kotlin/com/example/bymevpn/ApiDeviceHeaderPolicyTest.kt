package com.example.bymevpn

import com.example.bymevpn.data.api.ApiDeviceHeaderPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiDeviceHeaderPolicyTest {
    @Test
    fun legacyRequestsMatchWindowsDeviceIdentityScope() {
        assertTrue(ApiDeviceHeaderPolicy.includeHwid("GET", "/servers", false))
        assertTrue(ApiDeviceHeaderPolicy.includeHwid("POST", "/vpn/session", false))
        assertTrue(ApiDeviceHeaderPolicy.includeHwid("GET", "/devices", false))
        assertFalse(ApiDeviceHeaderPolicy.includeHwid("GET", "/subscription/status", false))
        assertFalse(ApiDeviceHeaderPolicy.includeHwid("POST", "/auth/google/nonce", false))
        assertFalse(ApiDeviceHeaderPolicy.includeHwid("POST", "/auth/telegram/check-session", false))

        assertFalse(ApiDeviceHeaderPolicy.includeDeviceMetadata("GET", "/servers", false))
        assertFalse(ApiDeviceHeaderPolicy.includeDeviceMetadata("GET", "/subscription/status", false))
        assertTrue(ApiDeviceHeaderPolicy.includeDeviceMetadata("GET", "/devices", false))
        assertTrue(ApiDeviceHeaderPolicy.includeDeviceMetadata("POST", "/vpn/session", false))
    }

    @Test
    fun explicitSignedEntitlementBuildRetainsIdentityOnAllRequests() {
        assertTrue(ApiDeviceHeaderPolicy.includeHwid("GET", "/subscription/status", true))
        assertTrue(ApiDeviceHeaderPolicy.includeHwid("POST", "/auth/google", true))
        assertTrue(ApiDeviceHeaderPolicy.includeDeviceMetadata("POST", "/auth/telegram/check-session", true))
    }
}
