package com.example.bymevpn

import com.example.bymevpn.data.api.PlanItem
import com.example.bymevpn.data.api.DeviceItem
import com.example.bymevpn.data.api.ApiJsonParsers
import com.example.bymevpn.screens.PlanDuration
import com.example.bymevpn.screens.matchesConfirmedPlan
import com.example.bymevpn.screens.visibleDeviceCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentPlanValidationTest {
    @Test
    fun deviceCounterAlwaysIncludesThisPhoneAndDoesNotDoubleCountIt() {
        assertEquals(1, visibleDeviceCount(emptyList()))
        assertEquals(1, visibleDeviceCount(listOf(DeviceItem("phone", "Pixel", "Android", "now", isCurrent = true))))
        assertEquals(2, visibleDeviceCount(listOf(DeviceItem("tablet", "Tablet", "Android", "now"))))
        assertEquals(2, visibleDeviceCount(listOf(
            DeviceItem("phone", "Pixel", "Android", "now", isCurrent = true),
            DeviceItem("tablet", "Tablet", "Android", "now")
        )))
    }

    @Test
    fun subscriptionDeviceLimitUsesServerFieldOrTierBoundPlanCode() {
        assertEquals(5, ApiJsonParsers.parseSubscriptionStatus(org.json.JSONObject("{\"plan_code\":\"12m_d5\"}")).maxDevices)
        assertEquals(10, ApiJsonParsers.parseSubscriptionStatus(org.json.JSONObject("{\"plan_code\":\"1m_d10\"}")).maxDevices)
        assertEquals(2, ApiJsonParsers.parseSubscriptionStatus(org.json.JSONObject("{\"plan_code\":\"3m_d5\",\"max_devices\":2}")).maxDevices)
        assertEquals(0, ApiJsonParsers.parseSubscriptionStatus(org.json.JSONObject("{\"plan_code\":\"year\"}")).maxDevices)
    }

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

    @Test
    fun planParserKeepsPricesVisibleButRequiresExplicitCheckoutCapability() {
        val disabled = ApiJsonParsers.parsePlans(org.json.JSONObject("""
            {"payment_enabled":false,"plans":[{"code":"12m_d5","name":"12+3","price_rub":1185,"days":450,"devices":5}]}
        """.trimIndent())).single()
        assertEquals(1185, disabled.priceRub)
        assertFalse(disabled.purchaseEnabled)
        assertTrue(PlanDuration.Month15.matchesConfirmedPlan(disabled, 5))

        val enabled = ApiJsonParsers.parsePlans(org.json.JSONObject("""
            {"payment_enabled":true,"plans":[{"code":"12m_d5","name":"12+3","price_rub":1185,"days":450,"devices":5}]}
        """.trimIndent())).single()
        assertTrue(enabled.purchaseEnabled)
        assertTrue(PlanDuration.Month15.matchesConfirmedPlan(enabled, 5))
    }

    @Test
    fun missingCheckoutCapabilityDefaultsToDisabled() {
        val plan = ApiJsonParsers.parsePlans(org.json.JSONObject("""
            {"plans":[{"code":"12m_d5","name":"12+3","price_rub":1185,"days":450,"devices":5}]}
        """.trimIndent())).single()
        assertEquals(1185, plan.priceRub)
        assertFalse(plan.purchaseEnabled)
    }
}
