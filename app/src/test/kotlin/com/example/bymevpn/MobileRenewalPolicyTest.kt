package com.example.bymevpn

import com.example.bymevpn.data.api.MobileRenewalPolicy
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileRenewalPolicyTest {
    @Test fun confirmedRenewalCanBeCancelled() {
        val payload = JSONObject().put("source", "server").put("auto_renew", true).put("can_cancel", true)
        assertTrue(MobileRenewalPolicy.parse(payload).canCancel)
    }

    @Test fun missingOrFakeBillingStateNeverEnablesCancellation() {
        val unknown = JSONObject().put("can_cancel", true).put("source", "server")
        assertThrows(IllegalArgumentException::class.java) { MobileRenewalPolicy.parse(unknown) }
        val untrusted = JSONObject().put("auto_renew", true).put("can_cancel", true)
        assertThrows(IllegalArgumentException::class.java) { MobileRenewalPolicy.parse(untrusted) }
        val disabled = JSONObject().put("source", "server").put("auto_renew", false).put("can_cancel", true)
        assertEquals(false, MobileRenewalPolicy.parse(disabled).canCancel)
    }
}
