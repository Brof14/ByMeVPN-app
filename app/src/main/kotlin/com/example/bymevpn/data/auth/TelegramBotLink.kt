package com.example.bymevpn.data.auth

import java.net.URI

internal fun isByMeTelegramBotLink(rawUrl: String): Boolean = try {
    val uri = URI(rawUrl)
    uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals("t.me", ignoreCase = true) &&
        uri.userInfo == null && uri.port == -1 && uri.fragment == null &&
        uri.path.equals("/ByMeVPNBot", ignoreCase = true) &&
        (uri.rawQuery?.matches(Regex("start=[A-Za-z0-9_-]+")) == true)
} catch (_: Exception) {
    false
}
