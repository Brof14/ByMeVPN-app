package com.example.bymevpn

import com.example.bymevpn.data.subscription.XraySubscriptionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Base64

class XraySubscriptionParserTest {

    @Test
    fun testParseVlessSubscription() {
        val link1 = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@138.124.240.14:16287?security=reality&flow=xtls-rprx-vision&pbk=fakeKey123&sid=12345678&sni=google.com&fp=chrome#%D0%93%D0%B5%D1%80%D0%BC%D0%B0%D0%BD%D0%B8%D1%8F%20Frankfurt"
        val link2 = "vless://11112222-3333-4444-5555-666677778888@138.124.240.14:35102?security=reality&flow=xtls-rprx-vision&pbk=fakeKey456&sid=87654321&sni=microsoft.com&fp=chrome#Netherlands%20Amsterdam"

        val rawText = "$link1\n$link2"
        val base64Payload = Base64.getEncoder().encodeToString(rawText.toByteArray())

        val userInfoHeader = "upload=1048576; download=5242880; total=107374182400; expire=1735689600"

        val result = XraySubscriptionParser.parse(base64Payload, userInfoHeader)

        assertEquals(2, result.servers.size)
        assertEquals(2, result.configs.size)

        val server1 = result.servers[0]
        assertEquals("Германия", server1.country)
        assertEquals("🇩🇪", server1.flag)
        assertEquals("Германия Frankfurt", server1.city)

        val config1 = result.configs[server1.nodeCode]
        assertNotNull(config1)
        assertEquals("138.124.240.14", config1?.server)
        assertEquals(16287, config1?.port)
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", config1?.uuid)
        assertEquals("fakeKey123", config1?.publicKey)
        assertEquals("12345678", config1?.shortId)
        assertEquals("google.com", config1?.serverName)

        val server2 = result.servers[1]
        assertEquals("Нидерланды", server2.country)
        assertEquals("🇳🇱", server2.flag)

        assertEquals(1048576L, result.uploadBytes)
        assertEquals(5242880L, result.downloadBytes)
        assertEquals(1735689600L, result.expireTimestampSec)
    }
}
