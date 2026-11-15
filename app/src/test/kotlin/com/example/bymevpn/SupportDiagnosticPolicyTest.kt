package com.example.bymevpn

import com.example.bymevpn.data.api.SupportDiagnosticPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportDiagnosticPolicyTest {
    @Test fun diagnosticsClassifyWithoutEchoingSecretsOrUrls() {
        val secret = "vless://super-secret@203.0.113.10:443?pbk=private"
        val result = SupportDiagnosticPolicy.classify(secret)
        assertEquals("other", result.stage)
        assertEquals("GENERAL_SUPPORT_REQUEST", result.errorCode)
        assertFalse(result.errorCode.contains("secret"))
    }

    @Test fun errorCodeIsWellDefinedForReportedFaults() {
        assertEquals("VPN_TUN_UNAVAILABLE", SupportDiagnosticPolicy.classify("Failed to establish TUN interface").errorCode)
        assertEquals("VPN_PERMISSION_DENIED", SupportDiagnosticPolicy.classify("VPN permission revoked").errorCode)
        assertEquals("VPN_DNS_UNAVAILABLE", SupportDiagnosticPolicy.classify("DNS failed").errorCode)
        assertTrue(SupportDiagnosticPolicy.isValidReport("connect", "VPN_TUN_UNAVAILABLE"))
        assertFalse(SupportDiagnosticPolicy.isValidReport("connect", "https://example.invalid/key"))
        assertFalse(SupportDiagnosticPolicy.isValidReport("private", "GENERAL_SUPPORT_REQUEST"))
    }
}
