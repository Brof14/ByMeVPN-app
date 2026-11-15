package com.example.bymevpn.data.api

import java.util.Locale

/** Standardized reports only: never upload raw exceptions, URLs, tokens or VPN configs. */
internal object SupportDiagnosticPolicy {
    data class Diagnostic(val stage: String, val errorCode: String)

    fun classify(message: String?): Diagnostic {
        val lower = message.orEmpty().lowercase(Locale.ROOT)
        return when {
            "permission" in lower || "разрешен" in lower || "разрешени" in lower ->
                Diagnostic("connect", "VPN_PERMISSION_DENIED")
            "tun" in lower || "туннел" in lower ->
                Diagnostic("connect", "VPN_TUN_UNAVAILABLE")
            "dns" in lower ->
                Diagnostic("dns", "VPN_DNS_UNAVAILABLE")
            "401" in lower || "unauthorized" in lower || "сессия" in lower ->
                Diagnostic("auth", "AUTH_SESSION_UNAVAILABLE")
            "device" in lower && "limit" in lower || "лимит" in lower && "устройств" in lower ->
                Diagnostic("subscription", "DEVICE_LIMIT_REACHED")
            "server" in lower || "сервер" in lower ->
                Diagnostic("connect", "VPN_SERVER_UNAVAILABLE")
            else -> Diagnostic("other", "GENERAL_SUPPORT_REQUEST")
        }
    }

    fun isValidReport(stage: String, errorCode: String): Boolean =
        stage in setOf("auth", "subscription", "import", "connect", "dns", "routing", "other") &&
        errorCode.matches(Regex("[A-Z][A-Z0-9_]{2,47}"))
}
