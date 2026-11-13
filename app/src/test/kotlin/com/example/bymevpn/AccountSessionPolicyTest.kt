package com.example.bymevpn

import com.example.bymevpn.data.AccountSessionPolicy
import com.example.bymevpn.data.api.ApiError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSessionPolicyTest {
    @Test
    fun onlyRejectedSessionResponsesClearTheCachedAccount() {
        assertTrue(AccountSessionPolicy.isExpired(ApiError("UNAUTHORIZED", "", 401)))
        assertTrue(AccountSessionPolicy.isExpired(ApiError("AUTH_REQUIRED", "", 401)))
        assertFalse(AccountSessionPolicy.isExpired(ApiError("PANEL_UNAVAILABLE", "", 503)))
        assertFalse(AccountSessionPolicy.isExpired(IllegalStateException("network unavailable")))
    }
}
