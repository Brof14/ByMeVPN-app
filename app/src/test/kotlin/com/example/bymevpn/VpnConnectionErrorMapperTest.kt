package com.example.bymevpn

import com.example.bymevpn.data.api.ApiError
import com.example.bymevpn.vpn.VpnConnectionErrorMapper
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnConnectionErrorMapperTest {
    @Test
    fun explicitDeviceLimitShowsActionableRussianMessage() {
        val message = VpnConnectionErrorMapper.deviceLimitMessage(
            ApiError("DEVICE_LIMIT_REACHED", "Too many devices"), isRussian = true
        )

        assertTrue(message?.contains("лимит устройств", ignoreCase = true) == true)
        assertTrue(message?.contains("Мои устройства") == true)
    }

    @Test
    fun maskedNotFoundExplainsPossibleDeviceLimit() {
        val message = VpnConnectionErrorMapper.deviceLimitMessage(
            ApiError("API_ERROR", "Not found", httpStatusCode = 404), isRussian = true
        )

        assertTrue(message?.contains("Возможно, достигнут лимит устройств") == true)
    }

    @Test
    fun unrelatedErrorsDoNotSuggestDeviceLimit() {
        assertNull(VpnConnectionErrorMapper.deviceLimitMessage(IllegalStateException("timeout"), isRussian = true))
        assertNull(VpnConnectionErrorMapper.deviceLimitMessage(ApiError("HTTP_503", "Unavailable"), isRussian = true))
    }
}
