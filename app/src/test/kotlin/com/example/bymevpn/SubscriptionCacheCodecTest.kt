package com.example.bymevpn

import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.storage.SubscriptionCacheCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionCacheCodecTest {
    @Test
    fun preservesShortLivedTrialBootstrapAcrossProcessRestart() {
        val original = SubscriptionStatus(
            status = "trial",
            secondsRemaining = 480,
            maxDevices = 2,
            entitlement = "signed-token",
            entitlementBootstrap = true,
            entitlementExpiresAt = 1_800_000_480L,
            lastFetchedAt = 1_800_000_000_000L
        )

        val restored = SubscriptionCacheCodec.decode(SubscriptionCacheCodec.encode(original))

        assertEquals("trial", restored?.status)
        assertEquals(2, restored?.maxDevices)
        assertTrue(restored?.entitlementBootstrap == true)
        assertEquals(original.entitlementExpiresAt, restored?.entitlementExpiresAt)
        assertEquals(original.entitlement, restored?.entitlement)
    }

    @Test
    fun legacyCacheCannotInventBootstrapMetadata() {
        val restored = SubscriptionCacheCodec.decode(
            """{"status":"trial","max_devices":2,"entitlement_bootstrap":true,"entitlement_expires_at":1800000480}"""
        )

        assertFalse(restored?.entitlementBootstrap ?: true)
        assertNull(restored?.entitlementExpiresAt)
    }
}
