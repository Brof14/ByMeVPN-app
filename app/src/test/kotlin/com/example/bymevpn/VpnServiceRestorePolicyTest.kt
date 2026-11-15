package com.example.bymevpn

import com.example.bymevpn.vpn.VpnServiceRestorePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnServiceRestorePolicyTest {
    @Test
    fun doesNotStartVpnWhenThereWasNoPreviousSession() {
        assertFalse(VpnServiceRestorePolicy.shouldRestore(previouslyActive = false, systemAlwaysOn = false))
    }

    @Test
    fun restoresAnPreviouslyActiveSessionOrSystemAlwaysOnVpn() {
        assertTrue(VpnServiceRestorePolicy.shouldRestore(previouslyActive = true, systemAlwaysOn = false))
        assertTrue(VpnServiceRestorePolicy.shouldRestore(previouslyActive = false, systemAlwaysOn = true))
    }
}
