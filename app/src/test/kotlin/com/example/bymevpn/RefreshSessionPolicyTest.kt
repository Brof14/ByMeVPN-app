package com.example.bymevpn

import com.example.bymevpn.data.api.RefreshSessionPolicy
import com.example.bymevpn.data.api.RefreshSessionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshSessionPolicyTest {
    @Test fun revocationRequiresExplicitInvalidRefreshTokenStatus() {
        for (status in listOf(400, 401)) {
            assertEquals(RefreshSessionResult.REJECTED, RefreshSessionPolicy.fromHttpStatus(status))
        }
    }

    @Test fun temporaryFailuresCannotDestroyStoredCredentials() {
        for (status in listOf(403, 404, 408, 409, 429, 500, 502, 503, 504)) {
            assertEquals(RefreshSessionResult.TEMPORARY_FAILURE, RefreshSessionPolicy.fromHttpStatus(status))
        }
    }

    @Test fun accountSwitchCannotRetrySensitiveRequestUsingAnotherIdentity() {
        assertTrue(RefreshSessionPolicy.sameAccountCanRetry("owner-A", "owner-A", "fresh-token"))
        assertFalse(RefreshSessionPolicy.sameAccountCanRetry("owner-A", "owner-B", "fresh-token"))
        assertFalse(RefreshSessionPolicy.sameAccountCanRetry("owner-A", null, "fresh-token"))
        assertFalse(RefreshSessionPolicy.sameAccountCanRetry(null, "owner-A", "fresh-token"))
        assertFalse(RefreshSessionPolicy.sameAccountCanRetry("owner-A", "owner-A", null))
        assertFalse(RefreshSessionPolicy.sameAccountCanRetry("owner-A", "owner-A", ""))
    }
}
