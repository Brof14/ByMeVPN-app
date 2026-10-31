package com.example.bymevpn.data.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens only the ByMeVPN bot link issued by the backend. */
object TelegramLinkOpener {
    fun open(context: Context, rawUrl: String): Boolean {
        if (!isByMeTelegramBotLink(rawUrl)) return false
        val uri = Uri.parse(rawUrl)

        for (packageName in listOf("org.telegram.messenger", "org.telegram.plus")) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(packageName))
                return true
            } catch (_: ActivityNotFoundException) {
                // Try the next installed client.
            } catch (_: SecurityException) {
                // An installed client may reject the link.
            }
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
