package com.example.bymevpn.data.subscription

import com.example.bymevpn.data.api.SubscriptionStatus
import org.json.JSONObject
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** Verifies the server's account- and installation-bound ES256 subscription snapshot. */
internal object EntitlementJwtVerifier {
    private const val ISSUER = "bymevpn-api"
    private const val AUDIENCE = "bymevpn-mobile"
    private const val MAX_TOKEN_TTL_SECONDS = 72L * 60 * 60
    private const val CLOCK_SKEW_SECONDS = 60L

    fun verify(
        jwt: String,
        publicKeyPem: String,
        expectedUserId: String,
        expectedHwid: String,
        expectedSubscription: SubscriptionStatus,
        nowSeconds: Long = System.currentTimeMillis() / 1000
    ): Boolean {
        return try {
            if (publicKeyPem.isBlank() || expectedUserId.isBlank() || expectedHwid.isBlank()) return false
            val parts = jwt.split('.')
            if (parts.size != 3) return false

            val header = JSONObject(String(decodeBase64(parts[0]) ?: return false, Charsets.UTF_8))
            if (header.optString("alg") != "ES256" || header.optString("typ") != "JWT") return false
            if (header.length() != 2) return false

            val claims = JSONObject(String(decodeBase64(parts[1]) ?: return false, Charsets.UTF_8))
            val issuedAt = exactLong(claims, "iat") ?: return false
            val notBefore = exactLong(claims, "nbf") ?: return false
            val expiresAt = exactLong(claims, "exp") ?: return false
            val maxDevices = exactLong(claims, "max_devices") ?: return false

            if (claims.optString("iss") != ISSUER || claims.optString("aud") != AUDIENCE) return false
            if (claims.optString("jti").isBlank()) return false
            if (claims.optString("sub") != expectedUserId) return false
            if (!constantTimeEquals(claims.optString("device_hash"), deviceHash(expectedUserId, expectedHwid))) return false
            if (claims.optString("status") != expectedSubscription.status) return false
            if (!planCodeMatches(claims, expectedSubscription.planCode)) return false
            if (maxDevices !in setOf(2L, 5L, 10L) || maxDevices != expectedSubscription.maxDevices.toLong()) return false
            if (issuedAt <= 0 || notBefore <= 0 || expiresAt <= 0) return false
            if (issuedAt > nowSeconds + CLOCK_SKEW_SECONDS || notBefore > nowSeconds + CLOCK_SKEW_SECONDS) return false
            if (expiresAt <= nowSeconds || expiresAt <= issuedAt || expiresAt - issuedAt > MAX_TOKEN_TTL_SECONDS) return false

            val rawSignature = decodeBase64(parts[2]) ?: return false
            if (rawSignature.size != 64) return false
            val signedBytes = "${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII)
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(parsePublicKey(publicKeyPem) ?: return false)
            verifier.update(signedBytes)
            verifier.verify(rawSignatureToDer(rawSignature))
        } catch (_: Exception) {
            false
        }
    }

    internal fun deviceHash(userId: String, hwid: String): String {
        val material = "bymevpn-entitlement-v1:$userId:${hwid.trim()}".toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(material)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun planCodeMatches(claims: JSONObject, expected: String?): Boolean {
        val claim = claims.opt("plan_code")
        return if (expected == null) claim == JSONObject.NULL else claim == expected
    }

    private fun exactLong(json: JSONObject, name: String): Long? {
        val value = json.opt(name) as? Number ?: return null
        val asLong = value.toLong()
        return asLong.takeIf { value.toDouble() == it.toDouble() }
    }

    private fun parsePublicKey(pem: String) = try {
        val encoded = pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val bytes = decodeBase64(encoded, allowPadding = true) ?: return null
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
    } catch (_: Exception) {
        null
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        return MessageDigest.isEqual(left.toByteArray(Charsets.US_ASCII), right.toByteArray(Charsets.US_ASCII))
    }

    /** Decodes standard or URL-safe Base64 without relying on Android API 26 Base64. */
    private fun decodeBase64(input: String, allowPadding: Boolean = false): ByteArray? {
        if (input.isEmpty()) return null
        val firstPadding = input.indexOf('=')
        val data = if (firstPadding >= 0) {
            if (!allowPadding) return null
            if (input.length - firstPadding > 2 || input.substring(firstPadding).any { it != '=' }) return null
            input.substring(0, firstPadding)
        } else input
        if (data.length % 4 == 1) return null

        val output = ArrayList<Byte>((data.length * 3) / 4)
        var buffer = 0
        var bits = 0
        for (char in data) {
            val value = when (char) {
                in 'A'..'Z' -> char - 'A'
                in 'a'..'z' -> char - 'a' + 26
                in '0'..'9' -> char - '0' + 52
                '+', '-' -> 62
                '/', '_' -> 63
                else -> return null
            }
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output.add(((buffer shr bits) and 0xff).toByte())
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        if (bits > 0 && buffer != 0) return null
        return output.toByteArray()
    }

    private fun rawSignatureToDer(raw: ByteArray): ByteArray {
        fun integer(offset: Int): ByteArray {
            val end = offset + 32
            var start = offset
            while (start < end - 1 && raw[start] == 0.toByte()) start++
            val needsPrefix = (raw[start].toInt() and 0x80) != 0
            val size = end - start + if (needsPrefix) 1 else 0
            val result = ByteArray(size + 2)
            result[0] = 0x02
            result[1] = size.toByte()
            System.arraycopy(raw, start, result, if (needsPrefix) 3 else 2, end - start)
            return if (needsPrefix) result.apply { this[2] = 0 } else result
        }

        val r = integer(0)
        val s = integer(32)
        val bodyLength = r.size + s.size
        return byteArrayOf(0x30, bodyLength.toByte()) + r + s
    }
}
