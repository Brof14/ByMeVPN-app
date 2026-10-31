package com.example.bymevpn

import com.example.bymevpn.data.api.PlanItem
import com.example.bymevpn.screens.PlanDuration
import com.example.bymevpn.screens.matchesConfirmedPlan
import com.example.bymevpn.screens.referencePriceRub
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingReferencePriceTest {
    @Test
    fun previewPricesMatchKnownBotCatalogUntilBackendConfirmation() {
        val expected = mapOf(
            2 to listOf(89, 316, 552, 885),
            5 to listOf(109, 396, 712, 1185),
            10 to listOf(129, 476, 872, 1485)
        )
        expected.forEach { (deviceLimit, amounts) ->
            assertEquals(amounts, PlanDuration.values().map { it.referencePriceRub(deviceLimit) })
        }
    }

    @Test
    fun paymentRequiresMatchingServerPlanAndDeviceCount() {
        val verified = PlanItem(
            code = "12m", name = "12+3", price = "1185 ₽", period = "15 месяцев",
            priceRub = 1185, days = 450, devices = 5
        )
        assertTrue(PlanDuration.Month15.matchesConfirmedPlan(verified, 5))
        assertFalse(PlanDuration.Month15.matchesConfirmedPlan(verified, 10))
        assertFalse(PlanDuration.Month8.matchesConfirmedPlan(verified, 5))
        assertFalse(PlanDuration.Month15.matchesConfirmedPlan(verified.copy(days = 360), 5))
        assertFalse(PlanDuration.Month15.matchesConfirmedPlan(verified.copy(priceRub = 0), 5))
        assertFalse(PlanDuration.Month15.matchesConfirmedPlan(verified.copy(code = "unknown"), 5))
    }
}
