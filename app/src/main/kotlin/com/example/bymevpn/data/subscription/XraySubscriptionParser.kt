package com.example.bymevpn.data.subscription

import android.util.Base64
import android.util.Log
import com.example.bymevpn.data.CountryFlags
import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.data.api.VpnSessionConfig
import java.net.URLDecoder
import java.net.URI
import java.util.Locale

data class ParsedSubscription(
    val servers: List<ServerNode>,
    val configs: Map<String, VpnSessionConfig>, // nodeCode -> VpnSessionConfig
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val expireTimestampSec: Long = 0L
)

/**
 * Universal parser for 3x-ui, Happ, and standard v2ray/xray subscriptions:
 * - Direct HTTP(S) Base64 subscriptions
 * - Direct single vless:// links
 * - Base64 text blobs containing multiple vless:// lines
 */
object XraySubscriptionParser {

    private const val TAG = "XraySubParser"
    private const val MAX_INPUT_BYTES = 1_048_576
    private const val MAX_CONFIGS = 64
    private const val MAX_LINES = 256

    fun parse(rawBody: String, userInfoHeader: String? = null): ParsedSubscription {
        require(rawBody.toByteArray(Charsets.UTF_8).size <= MAX_INPUT_BYTES) {
            "Subscription input is too large"
        }
        val decodedText = decodeContent(rawBody)
        require(decodedText.toByteArray(Charsets.UTF_8).size <= MAX_INPUT_BYTES) {
            "Decoded subscription is too large"
        }
        val lines = decodedText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
        require(lines.size <= MAX_LINES) { "Subscription contains too many lines" }

        val serverList = mutableListOf<ServerNode>()
        val configMap = mutableMapOf<String, VpnSessionConfig>()

        lines.forEachIndexed { index, line ->
            if (line.startsWith("vless://", ignoreCase = true)) {
                try {
                    val (serverNode, sessionConfig) = parseVlessUri(line, index)
                    serverList.add(serverNode)
                    configMap[serverNode.nodeCode] = sessionConfig
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse vless link at line $index")
                }
            } else if (line.startsWith("hysteria2://", ignoreCase = true) || line.startsWith("hy2://", ignoreCase = true)) {
                try {
                    val (serverNode, sessionConfig) = parseHysteria2Uri(line, index)
                    serverList.add(serverNode)
                    configMap[serverNode.nodeCode] = sessionConfig
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse hysteria2 link at line $index")
                }
            }
        }

        require(serverList.size <= MAX_CONFIGS) { "Subscription contains too many configurations" }

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
        val trimmed = raw.trim()
        if (trimmed.startsWith("vless://", ignoreCase = true) ||
            trimmed.startsWith("hysteria2://", ignoreCase = true) ||
            trimmed.startsWith("hy2://", ignoreCase = true)) {
            return trimmed
        }
        val clean = trimmed.replace("\\s".toRegex(), "")
        return try {
            val bytes = decodeBase64Compat(clean)
            if (bytes != null) {
                val decoded = String(bytes, Charsets.UTF_8)
                if (decoded.contains("vless://", ignoreCase = true) ||
                    decoded.contains("hysteria2://", ignoreCase = true) ||
                    decoded.contains("hy2://", ignoreCase = true)
                ) decoded else trimmed
            } else {
                trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun decodeBase64Compat(input: String): ByteArray? {
        if (input.isEmpty()) return null
        val androidDecoded = runCatching {
            Base64.decode(input, Base64.DEFAULT or Base64.NO_WRAP or Base64.URL_SAFE)
        }.getOrNull()
        if (androidDecoded != null && androidDecoded.isNotEmpty()) {
            return androidDecoded
        }
        // Fallback for local JVM unit tests (isReturnDefaultValues = true stubs android.util.Base64)
        // and pure API 24+ compatibility without calling java.util.Base64 directly.
        return decodeBase64PureKotlin(input)
    }

    private fun decodeBase64PureKotlin(input: String): ByteArray? {
        val normalized = input.replace('-', '+').replace('_', '/').trimEnd('=')
        if (normalized.isEmpty()) return null
        var buffer = 0
        var bitsLeft = 0
        val out = java.io.ByteArrayOutputStream((normalized.length * 3) / 4 + 1)
        for (ch in normalized) {
            val value = when (ch) {
                in 'A'..'Z' -> ch.code - 'A'.code
                in 'a'..'z' -> ch.code - 'a'.code + 26
                in '0'..'9' -> ch.code - '0'.code + 52
                '+' -> 62
                '/' -> 63
                else -> return null
            }
            buffer = (buffer shl 6) or value
            bitsLeft += 6
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.write((buffer shr bitsLeft) and 0xFF)
            }
        }
        return out.toByteArray()
    }

    private fun parseHysteria2Uri(uriString: String, index: Int): Pair<ServerNode, VpnSessionConfig> {
        val parts = parseProxyUri(uriString)
        val remark = parts.remark.ifBlank { "Hysteria 2 - Server ${index + 1}" }
        val password = parts.userInfo
        val host = parts.host
        val port = parts.port
        val queryParams = parts.queryParams

        val sni = queryParams["sni"] ?: host
        val obfs = queryParams["obfs"] ?: ""
        val obfsPassword = queryParams["obfs-password"] ?: ""
        val nodeCode = "node_${index}_hy2_${host.replace(".", "_")}_$port"
        val (country, countryCode, flag) = detectLocation(remark)
        val protoBadge = "Hysteria 2"
        val cleanRemark = if (remark.isNotBlank()) remark.trim() else "$country #${index + 1}"
        val dispName = if (cleanRemark.contains(protoBadge, ignoreCase = true) || cleanRemark.contains("hysteria", ignoreCase = true) || cleanRemark.contains("hy2", ignoreCase = true)) {
            cleanRemark
        } else {
            "$cleanRemark • $protoBadge"
        }

        val serverNode = ServerNode(
            nodeCode = nodeCode,
            country = country,
            countryCode = countryCode,
            city = cleanRemark,
            flag = flag,
            pingMs = -1,
            loadPercent = 0,
            isPremium = false,
            countryEn = country,
            cityEn = cleanRemark,
            protocol = protoBadge,
            remark = cleanRemark,
            displayName = dispName
        )

        val config = VpnSessionConfig(
            protocol = "hysteria2",
            server = host,
            port = port,
            uuid = password,
            encryption = obfs,
            flow = obfsPassword,
            security = "tls",
            serverName = sni,
            publicKey = "",
            shortId = "",
            fingerprint = queryParams["fp"] ?: "firefox",
            network = "udp",
            alpn = queryParams["alpn"] ?: ""
        )

        return Pair(serverNode, config)
    }

    private fun parseVlessUri(uriString: String, index: Int): Pair<ServerNode, VpnSessionConfig> {
        val parts = parseProxyUri(uriString)
        val remark = parts.remark.ifBlank { "Server ${index + 1}" }
        val uuid = parts.userInfo
        val host = parts.host
        val port = parts.port
        val queryParams = parts.queryParams

        val security = queryParams["security"] ?: if (queryParams.containsKey("pbk")) "reality" else "tls"
        val flow = queryParams["flow"] ?: ""
        val pbk = queryParams["pbk"] ?: ""
        val sid = queryParams["sid"] ?: ""
        val sni = queryParams["sni"] ?: queryParams["host"] ?: host
        val fp = queryParams["fp"] ?: "firefox"
        val type = queryParams["type"] ?: "tcp"
        val encryption = queryParams["encryption"] ?: "none"

        val nodeCode = "node_${index}_${host.replace(".", "_")}_$port"
        val (country, countryCode, flag) = detectLocation(remark)
        val secLower = security.lowercase()
        val protoBadge = if (secLower == "reality") "VLESS Reality" else if (secLower == "tls") "VLESS TLS" else "VLESS"
        val cleanRemark = if (remark.isNotBlank()) remark.trim() else "$country #${index + 1}"
        val dispName = if (cleanRemark.contains(protoBadge, ignoreCase = true) || cleanRemark.contains("reality", ignoreCase = true) || cleanRemark.contains("vless", ignoreCase = true)) {
            cleanRemark
        } else {
            "$cleanRemark • $protoBadge"
        }

        val serverNode = ServerNode(
            nodeCode = nodeCode,
            country = country,
            countryCode = countryCode,
            city = cleanRemark,
            flag = flag,
            pingMs = -1, // -1 means not measured yet
            loadPercent = 0,
            isPremium = false,
            countryEn = country,
            cityEn = cleanRemark,
            protocol = protoBadge,
            remark = cleanRemark,
            displayName = dispName
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
            network = type,
            transportPath = queryParams["servicename"] ?: queryParams["path"] ?: "",
            transportHost = queryParams["host"] ?: "",
            transportMode = queryParams["mode"] ?: "",
            alpn = queryParams["alpn"] ?: ""
        )

        return Pair(serverNode, config)
    }

    private data class ProxyUriParts(
        val userInfo: String,
        val host: String,
        val port: Int,
        val remark: String,
        val queryParams: Map<String, String>
    )

    /**
     * Read the actual endpoint from the URI. Splitting on ':' corrupts IPv6, and
     * silently defaulting a malformed/missing port to 443 may target the HTTPS API.
     */
    private fun parseProxyUri(raw: String): ProxyUriParts {
        val uri = URI(raw)
        require(uri.scheme?.lowercase(Locale.ROOT) in setOf("vless", "hysteria2", "hy2")) {
            "Unsupported subscription protocol"
        }
        val user = uri.rawUserInfo?.let(::decodeUriPart).orEmpty()
        require(user.isNotBlank()) { "VPN credentials missing from URI" }
        val host = uri.host?.removePrefix("[")?.removeSuffix("]")?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("VPN host missing or malformed")
        require(uri.port in 1..65535) { "VPN port missing or invalid" }
        val query = mutableMapOf<String, String>()
        uri.rawQuery?.split("&")?.filter { it.isNotEmpty() }?.forEach { param ->
            val pair = param.split("=", limit = 2)
            val key = decodeUriPart(pair[0]).lowercase(Locale.ROOT)
            query[key] = if (pair.size == 2) decodeUriPart(pair[1]) else ""
        }
        return ProxyUriParts(
            userInfo = user,
            host = host,
            port = uri.port,
            remark = uri.rawFragment?.let(::decodeUriPart).orEmpty(),
            queryParams = query
        )
    }

    // URLDecoder treats '+' as a form-space; URI components use it literally.
    private fun decodeUriPart(value: String): String =
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")

    private fun detectLocation(remark: String): Triple<String, String, String> {
        (CountryFlags.regionIn(remark) ?: CountryFlags.regionInCountryName(remark))?.let { code ->
            val country = Locale("", code).getDisplayCountry(Locale("ru"))
            return Triple(country.ifBlank { "Сервер" }, code, CountryFlags.emoji(code) ?: "🌐")
        }
        val lower = remark.lowercase(Locale.ROOT)
        fun hasCode(code: String): Boolean = Regex("(?<![\\p{L}])" + code + "(?![\\p{L}])", RegexOption.IGNORE_CASE).containsMatchIn(remark)
        return when {
            lower.contains("герман") || lower.contains("germany") || hasCode("de") ->
                Triple("Германия", "DE", "🇩🇪")
            lower.contains("нидерланд") || lower.contains("netherlands") || hasCode("nl") || lower.contains("amsterdam") ->
                Triple("Нидерланды", "NL", "🇳🇱")
            lower.contains("финлянд") || lower.contains("finland") || hasCode("fi") || lower.contains("helsinki") ->
                Triple("Финляндия", "FI", "🇫🇮")
            lower.contains("швеци") || lower.contains("sweden") || hasCode("se") || lower.contains("stockholm") ->
                Triple("Швеция", "SE", "🇸🇪")
            lower.contains("сша") || lower.contains("usa") || hasCode("us") || lower.contains("york") || lower.contains("miami") ->
                Triple("США", "US", "🇺🇸")
            lower.contains("турци") || lower.contains("turkey") || hasCode("tr") || lower.contains("istanbul") ->
                Triple("Турция", "TR", "🇹🇷")
            lower.contains("британи") || hasCode("uk") || hasCode("gb") || lower.contains("london") ->
                Triple("Великобритания", "GB", "🇬🇧")
            lower.contains("франци") || lower.contains("france") || hasCode("fr") || lower.contains("paris") ->
                Triple("Франция", "FR", "🇫🇷")
            lower.contains("польш") || lower.contains("poland") || hasCode("pl") || lower.contains("warsaw") ->
                Triple("Польша", "PL", "🇵🇱")
            lower.contains("росси") || lower.contains("russia") || hasCode("ru") || lower.contains("moscow") ->
                Triple("Россия", "RU", "🇷🇺")
            else ->
                Triple("Сервер", "", "🌐")
        }
    }
}
