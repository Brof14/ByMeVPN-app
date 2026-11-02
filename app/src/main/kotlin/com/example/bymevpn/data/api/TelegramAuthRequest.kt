package com.example.bymevpn.data.api

import org.json.JSONObject
import java.net.URLEncoder

/** Request contract for the short-lived Telegram login state. */
internal object TelegramAuthRequest {
    const val CHECK_SESSION_PATH = "/auth/telegram/check-session"

    fun sessionBody(token: String): JSONObject = JSONObject().put("token", token)

    fun legacyGetPath(token: String): String =
        "$CHECK_SESSION_PATH?token=${URLEncoder.encode(token, Charsets.UTF_8.name())}"

    fun shouldFallbackToLegacyGet(httpStatusCode: Int?): Boolean = httpStatusCode == 405
}
