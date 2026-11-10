package com.example.bymevpn.data.auth

import java.net.URI

/** Accept only the first-party handoff that asks the app to start Telegram sign-in. */
fun isTelegramLoginHandoff(rawUrl: String): Boolean {
    return try {
        val uri = URI(rawUrl)
        uri.scheme == "bymevpn" && uri.host == "login" &&
            uri.rawFragment == null && uri.rawQuery == "auth=telegram"
    } catch (_: Exception) {
        false
    }
}
