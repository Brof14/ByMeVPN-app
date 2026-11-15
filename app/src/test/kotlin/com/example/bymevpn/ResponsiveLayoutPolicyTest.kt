package com.example.bymevpn

import com.example.bymevpn.components.VpnButtonLayoutPolicy
import com.example.bymevpn.screens.SignInLayoutPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveLayoutPolicyTest {
    @Test fun powerButtonDoesNotConsumeCompactDisplay() {
        assertEquals(205, VpnButtonLayoutPolicy.diameterDp(360, 500))
        assertEquals(236, VpnButtonLayoutPolicy.diameterDp(320, 576))
        assertEquals(270, VpnButtonLayoutPolicy.diameterDp(412, 915))
        assertTrue(VpnButtonLayoutPolicy.diameterDp(280, 400) <= 210)
    }

    @Test fun loginHeroAdaptsForSmallScreensOrAccessibilityText() {
        assertTrue(SignInLayoutPolicy.isCompact(640, 1f))
        assertTrue(SignInLayoutPolicy.isCompact(800, 1.5f))
        assertFalse(SignInLayoutPolicy.isCompact(850, 1f))
    }
}
