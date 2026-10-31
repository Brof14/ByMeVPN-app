package com.example.bymevpn.data.subscription

import java.net.URL

internal data class PublicSubscriptionUrl(
    val url: URL,
    val rewritten: Boolean
)

/**
 * Turns a subscription URL received from the backend into a URL a phone can actually use.
 *
 * Backends that read the panel locally sometimes return `http://127.0.0.1:2096/sub/...`.
 * Such a URL points at the phone itself and can never resolve. The same path is requested
 * from the public backend host over HTTPS; the token stays inside the subscription path
 * and is never logged.
 */
internal object SubscriptionUrlPolicy {

    fun resolve(raw: String, apiBaseUrl: String): PublicSubscriptionUrl {
        val parsed = URL(raw)
        val host = parsed.host.orEmpty()
        val isLoopback = host.isBlank() ||
            host.equals("localhost", ignoreCase = true) ||
            host.startsWith("127.") ||
            host == "::1" ||
            host == "0.0.0.0"

        if (!isLoopback && parsed.protocol.equals("https", ignoreCase = true)) {
            return PublicSubscriptionUrl(parsed, rewritten = false)
        }

        val apiHost = try { URL(apiBaseUrl).host } catch (_: Exception) { null }
        if (isLoopback && !apiHost.isNullOrBlank()) {
            val port = if (parsed.port > 0) parsed.port else 2096
            val file = parsed.file.ifBlank { parsed.path }
            return PublicSubscriptionUrl(URL("https", apiHost, port, file), rewritten = true)
        }
        if (parsed.protocol.equals("http", ignoreCase = true) && host.isNotBlank()) {
            val upgraded = if (parsed.port > 0) {
                URL("https", host, parsed.port, parsed.file)
            } else {
                URL("https", host, parsed.file)
            }
            return PublicSubscriptionUrl(upgraded, rewritten = true)
        }
        return PublicSubscriptionUrl(parsed, rewritten = false)
    }
}
