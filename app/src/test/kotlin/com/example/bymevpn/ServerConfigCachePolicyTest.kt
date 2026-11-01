package com.example.bymevpn

import com.example.bymevpn.data.storage.ServerConfigCachePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConfigCachePolicyTest {
    @Test
    fun acceptsConfigThroughSixHourRefreshWindow() {
        val now = 100_000_000L

        assertTrue(ServerConfigCachePolicy.isFresh(now, now))
        assertTrue(ServerConfigCachePolicy.isFresh(now - ServerConfigCachePolicy.MAX_AGE_MS, now))
    }

    @Test
    fun expiresOldLegacyAndFutureDatedConfigs() {
        val now = 100_000_000L

        assertFalse(ServerConfigCachePolicy.isFresh(0L, now))
        assertFalse(ServerConfigCachePolicy.isFresh(now - ServerConfigCachePolicy.MAX_AGE_MS - 1L, now))
        assertFalse(ServerConfigCachePolicy.isFresh(now + 1L, now))
    }
}
