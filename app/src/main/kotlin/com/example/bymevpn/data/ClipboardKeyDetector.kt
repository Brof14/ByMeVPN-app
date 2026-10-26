package com.example.bymevpn.data

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import com.example.bymevpn.data.storage.SecureTokenStorage

object ClipboardKeyDetector {

    /**
     * Checks device clipboard for ByMeVPN subscription link, VLESS key, or Hysteria2 link.
     * Returns trimmed string if valid key is detected and not already active/dismissed.
     */
    fun detectKey(context: Context): String? {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
            if (!clipboard.hasPrimaryClip()) return null

            val description = clipboard.primaryClipDescription ?: return null
            if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
            ) {
                return null
            }

            val clip = clipboard.primaryClip ?: return null
            if (clip.itemCount == 0) return null

            val rawText = clip.getItemAt(0)?.text?.toString()?.trim() ?: return null
            if (rawText.length < 15 || rawText.length > 4096) return null

            val isValidFormat = rawText.startsWith("vless://", ignoreCase = true) ||
                rawText.startsWith("hysteria2://", ignoreCase = true) ||
                rawText.startsWith("hy2://", ignoreCase = true) ||
                rawText.startsWith("bymevpn://", ignoreCase = true) ||
                (rawText.startsWith("http://", ignoreCase = true) || rawText.startsWith("https://", ignoreCase = true)) &&
                (rawText.contains("bymevpn") || rawText.contains(":2096") || rawText.contains("/yay/")) ||
                rawText.startsWith("dmxlc3M6", ignoreCase = true) // base64 vless:

            if (!isValidFormat) return null

            val storage = SecureTokenStorage.getInstance(context)
            val currentSub = storage.getSubscriptionUrl()
            val dismissed = storage.getDismissedClipboardKey()

            // Don't prompt if already configured or previously dismissed
            if (rawText == currentSub || rawText == dismissed) {
                return null
            }

            return rawText
        } catch (_: Exception) {
            return null
        }
    }

    fun dismissKey(context: Context, key: String) {
        SecureTokenStorage.getInstance(context).setDismissedClipboardKey(key)
    }
}
