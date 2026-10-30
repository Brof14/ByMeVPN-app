package com.example.bymevpn

import com.example.bymevpn.data.subscription.XraySubscriptionParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64

class RealUserSubscriptionTest {
    @Test
    fun parsesSyntheticMixedSubscription() {
        val uuid = "11111111-2222-3333-4444-555555555555"
        val input = listOf(
            "vless://$uuid@example.org:443?security=tls&sni=example.org#Germany-1",
            "vless://$uuid@example.org:8443?security=reality&pbk=test-key&sid=1234&sni=example.org#Germany-2",
            "hysteria2://test-password@example.org:443?sni=example.org#Germany-3",
            "vless://$uuid@example.org:9443?security=tls&type=xhttp&path=%2Fvpn#Germany-4"
        ).joinToString("\n")
        val parsed = XraySubscriptionParser.parse(Base64.getEncoder().encodeToString(input.toByteArray()))
        assertEquals(4, parsed.servers.size)
        assertEquals(4, parsed.configs.size)
        assertEquals(1, parsed.configs.values.count { it.protocol == "hysteria2" })
        assertEquals(1, parsed.configs.values.count { it.network == "xhttp" })
    }
}
