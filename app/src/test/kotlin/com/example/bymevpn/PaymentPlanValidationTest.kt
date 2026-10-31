package com.example.bymevpn

import com.example.bymevpn.data.api.PlanItem
import com.example.bymevpn.screens.PlanDuration
import com.example.bymevpn.screens.matchesConfirmedPlan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentPlanValidationTest {
    @Test
    fun paymentRequiresMatchingServerPlanAndDeviceCount() {
        val verified = PlanItem(
            code = "12m_d5", name = "12+3", price = "1185 ₽", period = "15 месяцев",
            priceRub = 1185, days = 450, devices = 5
        )
        assertTrue(PlanDuration.Month15.matchesConfirmedPlan(verified, 5))
        assertFalse(PlanDuration.Month15.matchesConfirmedPlan(verified, 10))
        assertFalse(PlanDuration.Month8.matchesConfirmedPlan(verified, 5))
        assertFalse(PlanDuration.Month15.matchesConfirmedPlan(verified.copy(days = 360), 5))
        assertFalse(PlanDuration.Month15.matchesConfirmedPlan(verified.copy(priceRub = 0), 5))
        assertFalse(PlanDuration.Month15.matchesConfirmedPlan(verified.copy(code = "unknown"), 5))
        assertFalse(PlanDuration.Month15.matchesConfirmedPlan(verified.copy(code = "12m_d10"), 5))
    }
}
