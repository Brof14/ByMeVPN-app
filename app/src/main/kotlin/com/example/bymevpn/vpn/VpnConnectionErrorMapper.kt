package com.example.bymevpn.vpn

import com.example.bymevpn.BuildConfig
import com.example.bymevpn.data.api.ApiError

/** Converts device-limit rejections into a useful message without exposing raw server responses. */
internal object VpnConnectionErrorMapper {
    fun subscriptionRequiredMessage(
        isRussian: Boolean,
        trialAvailable: Boolean,
        accessActive: Boolean = false,
        externalCheckoutEnabled: Boolean = BuildConfig.EXTERNAL_CHECKOUT_ENABLED
    ): String =
        if (isRussian) {
            when {
                accessActive -> "Подписка активна, но сервер пока не подтвердил доступ. Обновите статус подписки и список серверов."
                trialAvailable -> "Сначала активируйте бесплатный пробный период на главном экране."
                externalCheckoutEnabled -> "Для подключения нужна активная подписка. Выберите тариф в приложении."
                else -> "Для подключения нужна активная подписка. Проверьте статус аккаунта в разделе «Аккаунт»."
            }
        } else {
            when {
                accessActive -> "Your subscription is active, but server access is not confirmed yet. Refresh your subscription and server list."
                trialAvailable -> "Activate the free trial on the home screen before connecting."
                externalCheckoutEnabled -> "An active subscription is required to connect. Choose a plan in the app."
                else -> "An active subscription is required to connect. Check your account status in Account."
            }
        }

    fun subscriptionRequiredMessage(
        error: Throwable,
        isRussian: Boolean,
        trialAvailable: Boolean,
        accessActive: Boolean = false,
        externalCheckoutEnabled: Boolean = BuildConfig.EXTERNAL_CHECKOUT_ENABLED
    ): String? {
        val apiError = generateSequence(error) { it.cause }
            .filterIsInstance<ApiError>()
            .firstOrNull() ?: return null
        if (!apiError.code.equals("SUBSCRIPTION_REQUIRED", ignoreCase = true) &&
            !apiError.message.contains("subscription_required", ignoreCase = true)
        ) return null
        return subscriptionRequiredMessage(isRussian, trialAvailable, accessActive, externalCheckoutEnabled)
    }

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
