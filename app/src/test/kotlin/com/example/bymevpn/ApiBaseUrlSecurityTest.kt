package com.example.bymevpn

import com.example.bymevpn.data.api.ApiConfig
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiBaseUrlSecurityTest {
    @Test
    fun apiUsesVerifiedPublicHttpsOriginWithoutCustomPort() {
        val uri = URI(ApiConfig.BASE_URL)

        assertEquals("https", uri.scheme)
        assertEquals("bymevpn.duckdns.org", uri.host)
        assertEquals(-1, uri.port)
        assertEquals("/api/v1", uri.path)
    }
}
