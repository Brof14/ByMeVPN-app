package com.example.bymevpn

import com.example.bymevpn.data.subscription.XraySubscriptionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealUserSubscriptionTest {

    @Test
    fun testParseRealUserSubscriptionAndLink() {
        val realBase64 = "dmxlc3M6Ly9jYWQwOGE1NS0yYzgxLTQ1M2UtYTc3ZS01ZDIzZWY1NDUzMmVAYnltZXZwbi5kdWNrZG5zLm9yZzo1MjUyMj9hbHBuPWgyJTJDaHR0cCUyRjEuMSZlbmNyeXB0aW9uPW5vbmUmZnA9ZmlyZWZveCZzZWN1cml0eT10bHMmc25pPWJ5bWV2cG4uZHVja2Rucy5vcmcmdHlwZT10Y3AjJUYwJTlGJTg3JUE5JUYwJTlGJTg3JUFBR2VybWFueS0xCnZsZXNzOi8vY2FkMDhhNTUtMmM4MS00NTNlLWE3N2UtNWQyM2VmNTQ1MzJlQGJ5bWV2cG4uZHVja2Rucy5vcmc6MTYyODc/ZW5jcnlwdGlvbj1ub25lJmZwPWZpcmVmb3gmcGJrPXc5OGNuMUJRTDlQM2tCODkyeGFETmF2dkxEZ0QxYVNzZDk4aVNWNWVaMU0mc2VjdXJpdHk9cmVhbGl0eSZzaWQ9NmZmNWYxMGE2OGNmZjM5ZSZzbmk9cmFkZW9uLmNvbSZzcHg9JTJGZTZjZDMyMzAyNzE3ODM3JnN1cHBvcnQteDI1NTE5bWxrZW03Njg9dHJ1ZSZ0eXBlPXRjcCMlRjAlOUYlODclQTklRjAlOUYlODclQUFHZXJtYW55LTIKaHlzdGVyaWEyOi8vYzc3OTQ3OThjYmFiNDJlMGIxNWNkZTg4NjZhMzY2ZDhAYnltZXZwbi5kdWNrZG5zLm9yZzo1OTc2Mz9hbHBuPWgzJmZwPWZpcmVmb3gmb2Jmcz1zYWxhbWFuZGVyJm9iZnMtcGFzc3dvcmQ9a3c1NGJiaHcxOHdoNWNwNiZzZWN1cml0eT10bHMmc25pPWJ5bWV2cG4uZHVja2Rucy5vcmcjJUYwJTlGJTg3JUE5JUYwJTlGJTg3JUFBR2VybWFueS0zCnZsZXNzOi8vY2FkMDhhNTUtMmM4MS00NTNlLWE3N2UtNWQyM2VmNTQ1MzJlQGJ5bWV2cG4uZHVja2Rucy5vcmc6MzUxMDI/YWxwbj1oMiUyQ2h0dHAlMkYxLjEmZW5jcnlwdGlvbj1ub25lJmV4dHJhPSU3QiUyMm1vZGUlMjIlM0ElMjJhdXRvJTIyJTJDJTIyeFBhZGRpbmdCeXRlcyUyMiUzQSUyMjEwMC0xMDAwJTIyJTdEJmZwPWNocm9tZSZob3N0PSZtb2RlPWF1dG8mcGF0aD0lMkYmc2VjdXJpdHk9dGxzJnNuaT1ieW1ldnBuLmR1Y2tkbnMub3JnJnR5cGU9eGh0dHAmeF9wYWRkaW5nX2J5dGVzPTEwMC0xMDAwIyVGMCU5RiU4NyVBOSVGMCU5RiU4NyVBQUdlcm1hbnktNAo="
        val userInfo = "upload=47894297808; download=258985909801; total=0; expire=0"

        val parsed = XraySubscriptionParser.parse(realBase64, userInfo)

        // Must extract 4 servers (3 VLESS + 1 Hysteria2)
        assertEquals(4, parsed.servers.size)
        assertEquals(4, parsed.configs.size)

        val server1 = parsed.servers.firstOrNull { it.city.contains("Germany-1") } ?: parsed.servers[0]
        assertEquals("Германия", server1.country)
        assertEquals("🇩🇪", server1.flag)
        assertTrue(server1.city.contains("Germany-1"))

        val config1 = parsed.configs[server1.nodeCode]
        assertNotNull(config1)
        assertTrue(config1?.server == "2.27.120.247" || config1?.server == "bymevpn.duckdns.org")
        assertEquals(52522, config1?.port)
        assertEquals("tls", config1?.security)

        val server2 = parsed.servers.firstOrNull { it.city.contains("Germany-2") } ?: parsed.servers[1]
        val config2 = parsed.configs[server2.nodeCode]
        assertNotNull(config2)
        assertEquals(16287, config2?.port)
        assertEquals("reality", config2?.security)
        assertEquals("w98cn1BQL9P3kB892xaDNavvLDgD1aSsd98iSV5eZ1M", config2?.publicKey)

        // Single vless link test
        val singleLink = "vless://cad08a55-2c81-453e-a77e-5d23ef54532e@bymevpn.duckdns.org:52522?alpn=h2%2Chttp%2F1.1&encryption=none&fp=firefox&security=tls&sni=bymevpn.duckdns.org&type=tcp#%F0%9F%87%A9%F0%9F%87%AAGermany-1"
        val singleParsed = XraySubscriptionParser.parse(singleLink)
        assertEquals(1, singleParsed.servers.size)
    }
}
