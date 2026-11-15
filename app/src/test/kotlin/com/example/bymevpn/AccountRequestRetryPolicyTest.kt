package com.example.bymevpn

import com.example.bymevpn.data.api.AccountRequestRetryPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRequestRetryPolicyTest {
    @Test
    fun tokenRotationWithinTheSameSessionCanBeRetried() {
        assertTrue(AccountRequestRetryPolicy.sameSession("account-session-A", "account-session-A"))
    }

    @Test
    fun aSwitchToAnotherAccountMustNeverReplayTheRequest() {
        // A replayed DELETE /me could otherwise delete the newly signed-in account.
        assertFalse(AccountRequestRetryPolicy.sameSession("account-session-A", "account-session-B"))
    }

    @Test
    fun anAbsentOrRevokedSessionCannotAuthorizeReplay() {
        assertFalse(AccountRequestRetryPolicy.sameSession(null, "account-session-B"))
        assertFalse(AccountRequestRetryPolicy.sameSession("", ""))
        assertFalse(AccountRequestRetryPolicy.sameSession("account-session-A", null))
    }
}
