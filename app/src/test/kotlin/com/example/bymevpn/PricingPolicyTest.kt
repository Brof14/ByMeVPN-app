package com.example.bymevpn

import com.example.bymevpn.data.api.PlanItem
import com.example.bymevpn.screens.PlanDuration
import com.example.bymevpn.screens.bestValuePlan
import com.example.bymevpn.screens.roundedMonthlyPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PricingPolicyTest {
    @Test
    fun bestValueRequiresCompleteServerConfirmedCatalog() {
        val plans = listOf(
            PlanItem(code = "1m_d2", name = "1 month", price = "100", period = "30d", priceRub = 100, days = 30, devices = 2),
            PlanItem(code = "3m_d2", name = "3+1 months", price = "300", period = "120d", priceRub = 300, days = 120, devices = 2),
            PlanItem(code = "6m_d2", name = "6+2 months", price = "500", period = "240d", priceRub = 500, days = 240, devices = 2),
            PlanItem(code = "12m_d2", name = "12+3 months", price = "900", period = "450d", priceRub = 900, days = 450, devices = 2)
        )
        assertEquals(PlanDuration.Month15, bestValuePlan(plans, 2))
        assertNull(bestValuePlan(plans.dropLast(1), 2))
    }

    @Test
    fun monthlyPriceRoundsUpInsteadOfUnderstatingCost() {
        assertEquals(34, roundedMonthlyPrice(100, 3))
        assertEquals(0, roundedMonthlyPrice(100, 0))
    }
}
