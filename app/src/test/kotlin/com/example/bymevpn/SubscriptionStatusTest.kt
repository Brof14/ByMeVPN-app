package com.example.bymevpn

import com.example.bymevpn.data.api.ApiConfig
import com.example.bymevpn.data.api.SubscriptionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionStatusTest {

    @Test
    fun testIsUsable_active() {
        val sub = SubscriptionStatus(status = "active")
        assertTrue(sub.isUsable)
    }

    @Test
    fun testIsUsable_trial() {
        val sub = SubscriptionStatus(status = "trial")
        assertTrue(sub.isUsable)
    }

    @Test
    fun testIsUsable_grace() {
        val sub = SubscriptionStatus(status = "grace")
        assertTrue(sub.isUsable)
    }

    @Test
    fun testIsUsable_expired() {
        val sub = SubscriptionStatus(status = "expired")
        assertFalse(sub.isUsable)
    }

    @Test
    fun testIsUsable_none() {
        val sub = SubscriptionStatus(status = "none")
        assertFalse(sub.isUsable)
    }

    @Test
    fun testOfflineDuration_72HoursRule() {
        val now = System.currentTimeMillis()
        val within72h = SubscriptionStatus(
            status = "active",
            lastFetchedAt = now - (70 * 3600 * 1000L) // 70 hours ago
        )
        val expiredOffline = SubscriptionStatus(
            status = "active",
            lastFetchedAt = now - (73 * 3600 * 1000L) // 73 hours ago
        )

        val isWithinAllowed = (now - within72h.lastFetchedAt) <= ApiConfig.MAX_OFFLINE_DURATION_MS
        val isExpiredAllowed = (now - expiredOffline.lastFetchedAt) <= ApiConfig.MAX_OFFLINE_DURATION_MS

        assertTrue(isWithinAllowed)
        assertFalse(isExpiredAllowed)
    }
}
