package com.example.bymevpn.vpn

import com.example.bymevpn.data.api.ApiError

/** Converts device-limit rejections into a useful message without exposing raw server responses. */
internal object VpnConnectionErrorMapper {
    fun deviceLimitMessage(error: Throwable, isRussian: Boolean): String? {
        val apiError = generateSequence(error) { it.cause }
            .filterIsInstance<ApiError>()
            .firstOrNull()

        val code = apiError?.code.orEmpty()
        val text = "$code ${apiError?.message.orEmpty()}"
        val explicitLimit = listOf("device_limit", "device limit", "too_many_devices", "hwid_limit", "maximum devices")
            .any { text.contains(it, ignoreCase = true) }
        // The subscription service intentionally masks some HWID-limit denials as HTTP 404.
        val maskedLimit = apiError?.httpStatusCode == 404 || code.equals("HTTP_404", ignoreCase = true)
        if (!explicitLimit && !maskedLimit) return null

        return if (isRussian) {
            if (explicitLimit) {
                "Достигнут лимит устройств. Откройте «Аккаунт» → «Мои устройства» и удалите устройство, которым больше не пользуетесь."
            } else {
                "Сервер не выдал доступ. Возможно, достигнут лимит устройств: откройте «Аккаунт» → «Мои устройства» и проверьте список."
            }
        } else {
            if (explicitLimit) {
                "Your device limit has been reached. Open Account → My devices and remove a device you no longer use."
            } else {
                "The server did not grant access. You may have reached your device limit: open Account → My devices and check the list."
            }
        }
    }
}
