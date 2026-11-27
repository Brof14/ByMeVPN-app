package com.example.bymevpn.data.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSessionCommitPolicyTest {
    @Test
    fun acceptsCurrentAuthenticatedSessionWithoutCachedProfileId() {
        assertTrue(AccountSessionCommitPolicy.canCommit("session-a", "session-a", true, false))
    }

    @Test
    fun rejectsResponseAfterAccountSwitch() {
        assertFalse(AccountSessionCommitPolicy.canCommit("session-a", "session-b", true, false))
    }

    @Test
    fun rejectsResponseAfterLogout() {
        assertFalse(AccountSessionCommitPolicy.canCommit("session-a", "session-a", false, false))
    }

    @Test
    fun rejectsAccountDataInExternalProfileMode() {
        assertFalse(AccountSessionCommitPolicy.canCommit("session-a", "session-a", true, true))
    }

    @Test
    fun rejectsMissingExpectedSession() {
        assertFalse(AccountSessionCommitPolicy.canCommit("", "session-a", true, false))
    }
}
