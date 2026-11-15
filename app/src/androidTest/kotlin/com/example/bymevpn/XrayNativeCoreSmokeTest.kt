package com.example.bymevpn

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.bymevpn.vpn.xray.XrayVpnEngine
import libv2ray.Libv2ray
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android/JNI initialization smoke test; does not prove remote VPN connectivity. */
@RunWith(AndroidJUnit4::class)
class XrayNativeCoreSmokeTest {
    @Test
    fun nativeXrayRuntimeAndAssetsInitializeOnEmulator() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        XrayVpnEngine.getInstance(context).initCoreEnv()
        assertNotNull(Libv2ray.checkVersionX())
    }
}
