package com.example.bymevpn.data.subscription

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.api.VpnSessionConfig
import java.net.URLDecoder

data class ParsedSubscription(
    val servers: List<ServerNode>,
    val configs: Map<String, VpnSessionConfig>, // nodeCode -> VpnSessionConfig
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val expireTimestampSec: Long = 0L
)

/**
 * Universal parser for 3x-ui, Happ, and v2ray standard subscriptions.
 * - Decodes Base64 subscription body into vless:// links
 * - Parses VLESS + Reality query parameters (pbk, sid, sni, flow, fp)
 * - Parses Subscription-Userinfo HTTP response header for data usage and expiration
 */
object XraySubscriptionParser {

    private const val TAG = "XraySubParser"

    fun parse(rawBody: String, userInfoHeader: String? = null): ParsedSubscription {
        val decodedText = decodeContent(rawBody)
        val lines = decodedText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        val serverList = mutableListOf<ServerNode>()
        val configMap = mutableMapOf<String, VpnSessionConfig>()

        lines.forEachIndexed { index, line ->
            if (line.startsWith("vless://", ignoreCase = true)) {
                try {
                    val (serverNode, sessionConfig) = parseVlessUri(line, index)
                    serverList.add(serverNode)
                    configMap[serverNode.nodeCode] = sessionConfig
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse vless link at line $index: ${e.message}")
                }
            }
        }

        // Parse Subscription-Userinfo header: "upload=1024; download=2048; total=107374182400; expire=1735689600"
        var upload = 0L
        var download = 0L
        var total = 0L
        var expire = 0L

        if (!userInfoHeader.isNullOrBlank()) {
            userInfoHeader.split(";").forEach { part ->
                val kv = part.trim().split("=")
                if (kv.size == 2) {
                    val key = kv[0].trim().lowercase()
                    val value = kv[1].trim().toLongOrNull() ?: 0L
                    when (key) {
                        "upload" -> upload = value
                        "download" -> download = value
                        "total" -> total = value
                        "expire" -> expire = value
                    }
                }
            }
        }

        return ParsedSubscription(
            servers = serverList,
            configs = configMap,
            uploadBytes = upload,
            downloadBytes = download,
            totalBytes = total,
            expireTimestampSec = expire
        )
    }

    private fun decodeContent(raw: String): String {
        val clean = raw.trim().replace("\\s".toRegex(), "")
        return try {
            val bytes = Base64.decode(clean, Base64.DEFAULT or Base64.NO_WRAP or Base64.URL_SAFE)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Might already be raw plain text lines
            raw
        }
    }

    private fun parseVlessUri(uriString: String, index: Int): Pair<ServerNode, VpnSessionConfig> {
        // Format: vless://UUID@HOST:PORT?query#REMARK
        val withoutScheme = uriString.removePrefix("vless://").removePrefix("VLESS://")
        val hashSplit = withoutScheme.split("#", limit = 2)
        val mainPart = hashSplit[0]
        val rawRemark = if (hashSplit.size > 1) hashSplit[1] else "Server ${index + 1}"
        val remark = try { URLDecoder.decode(rawRemark, "UTF-8") } catch (e: Exception) { rawRemark }

        val atSplit = mainPart.split("@", limit = 2)
        val uuid = atSplit[0]
        val hostAndRest = atSplit[1]

        val questionSplit = hostAndRest.split("?", limit = 2)
        val hostPortPart = questionSplit[0]
        val queryString = if (questionSplit.size > 1) questionSplit[1] else ""

        val hpSplit = hostPortPart.split(":")
        val host = hpSplit[0]
        val port = hpSplit.getOrNull(1)?.toIntOrNull() ?: 443

        val queryParams = mutableMapOf<String, String>()
        if (queryString.isNotEmpty()) {
            queryString.split("&").forEach { param ->
                val kv = param.split("=", limit = 2)
                val key = kv[0].lowercase()
                val value = if (kv.size > 1) {
                    try { URLDecoder.decode(kv[1], "UTF-8") } catch (e: Exception) { kv[1] }
                } else ""
                queryParams[key] = value
            }
        }

        val security = queryParams["security"] ?: "reality"
        val flow = queryParams["flow"] ?: "xtls-rprx-vision"
        val pbk = queryParams["pbk"] ?: ""
        val sid = queryParams["sid"] ?: ""
        val sni = queryParams["sni"] ?: queryParams["host"] ?: host
        val fp = queryParams["fp"] ?: "chrome"
        val type = queryParams["type"] ?: "tcp"
        val encryption = queryParams["encryption"] ?: "none"

        val nodeCode = "node_${index}_${host.replace(".", "_")}_$port"

        // Detect country and flag from remark
        val (country, countryCode, flag) = detectLocation(remark)

        val serverNode = ServerNode(
            nodeCode = nodeCode,
            country = country,
            countryCode = countryCode,
            city = remark,
            flag = flag,
            pingMs = 25 + (index * 5),
            loadPercent = 20 + (index * 7) % 60,
            isPremium = false,
            countryEn = country,
            cityEn = remark
        )

        val config = VpnSessionConfig(
            protocol = "vless",
            server = host,
            port = port,
            uuid = uuid,
            encryption = encryption,
            flow = flow,
            security = security,
            serverName = sni,
            publicKey = pbk,
            shortId = sid,
            fingerprint = fp,
            network = type
        )

        return Pair(serverNode, config)
    }

    private fun detectLocation(remark: String): Triple<String, String, String> {
        val lower = remark.lowercase()
        return when {
            lower.contains("герман") || lower.contains("germany") || lower.contains("de") || lower.contains("frankfurt") ->
                Triple("Германия", "DE", "🇩🇪")
            lower.contains("нидерланд") || lower.contains("netherlands") || lower.contains("nl") || lower.contains("amsterdam") ->
                Triple("Нидерланды", "NL", "🇳🇱")
            lower.contains("финлянд") || lower.contains("finland") || lower.contains("fi") || lower.contains("helsinki") ->
                Triple("Финляндия", "FI", "🇫🇮")
            lower.contains("сша") || lower.contains("usa") || lower.contains("united states") || lower.contains("us") ->
                Triple("США", "US", "🇺🇸")
            lower.contains("турци") || lower.contains("turkey") || lower.contains("tr") || lower.contains("istanbul") ->
                Triple("Турция", "TR", "🇹🇷")
            lower.contains("швеци") || lower.contains("sweden") || lower.contains("se") || lower.contains("stockholm") ->
                Triple("Швеция", "SE", "🇸🇪")
            lower.contains("польш") || lower.contains("poland") || lower.contains("pl") || lower.contains("warsaw") ->
                Triple("Польша", "PL", "🇵🇱")
            lower.contains("росси") || lower.contains("russia") || lower.contains("ru") || lower.contains("moscow") ->
                Triple("Россия", "RU", "🇷🇺")
            else ->
                Triple("Европа", "EU", "🌐")
        }
    }
}
