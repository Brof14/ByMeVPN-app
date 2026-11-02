package com.example.bymevpn.data.api

import org.json.JSONObject

/** Request contract for the short-lived Telegram login state. */
internal object TelegramAuthRequest {
    const val CHECK_SESSION_PATH = "/auth/telegram/check-session"

    fun sessionBody(token: String): JSONObject = JSONObject().put("token", token)
}
