package com.example.bymevpn

import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.subscription.EntitlementJwtVerifier
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class EntitlementJwtVerifierTest {
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    private val now = 1_800_000_000L
    private val userId = "user-42"
    private val hwid = "installation-id-123"
    private val subscription = SubscriptionStatus(
        status = "active",
        planCode = "1m_d5",
        maxDevices = 5
    )

    @Test
    fun validSignedEntitlementMatchesAccountDeviceAndSubscription() {
        assertTrue(verify(token()))
    }

    @Test
    fun rejectsWrongIssuerAudienceUserDeviceStatusPlanAndLimit() {
        val changes = listOf(
            "iss" to "other-api",
            "aud" to "other-client",
            "sub" to "another-user",
            "device_hash" to EntitlementJwtVerifier.deviceHash(userId, "other-device"),
            "status" to "expired",
            "plan_code" to "other-plan",
            "max_devices" to 10
        )
        changes.forEach { (claim, value) -> assertFalse("claim $claim must be rejected", verify(token(mapOf(claim to value)))) }
    }

    @Test
    fun rejectsExpiredFutureAndOverlongEntitlements() {
        assertFalse(verify(token(mapOf("exp" to now))))
        assertFalse(verify(token(mapOf("nbf" to now + 61))))
        assertFalse(verify(token(mapOf("iat" to now + 61, "nbf" to now + 61))))
        assertFalse(verify(token(mapOf("exp" to now + 72 * 60 * 60 + 1))))
    }

    @Test
    fun rejectsTamperedPayloadMalformedTokenAndMissingKey() {
        val valid = token()
        val parts = valid.split('.').toMutableList()
        parts[1] = base64Url(JSONObject(mapOf("sub" to "attacker")).toString().toByteArray())
        assertFalse(verify(parts.joinToString(".")))
        assertFalse(verify("not-a-jwt"))
        assertFalse(EntitlementJwtVerifier.verify(valid, "", userId, hwid, subscription, now))
    }

    private fun verify(jwt: String) = EntitlementJwtVerifier.verify(
        jwt = jwt,
        publicKeyPem = pem(keyPair.public.encoded),
        expectedUserId = userId,
        expectedHwid = hwid,
        expectedSubscription = subscription,
        nowSeconds = now
    )

    private fun token(overrides: Map<String, Any> = emptyMap()): String {
        val header = base64Url("""{"alg":"ES256","typ":"JWT"}""".toByteArray())
        val claims = JSONObject().apply {
            put("iss", "bymevpn-api")
            put("aud", "bymevpn-mobile")
            put("sub", userId)
            put("device_hash", EntitlementJwtVerifier.deviceHash(userId, hwid))
            put("status", subscription.status)
            put("plan_code", subscription.planCode)
            put("max_devices", subscription.maxDevices)
            put("iat", now - 10)
            put("nbf", now - 10)
            put("exp", now + 3600)
            put("jti", "test-token-id")
            overrides.forEach { (key, value) -> put(key, value) }
        }.toString()
        val signingInput = "$header.${base64Url(claims.toByteArray())}"
        val signer = Signature.getInstance("SHA256withECDSA").apply {
            initSign(keyPair.private)
            update(signingInput.toByteArray(Charsets.US_ASCII))
        }
        val rawSignature = derToRaw(signer.sign())
        return "$signingInput.${base64Url(rawSignature)}"
    }

    private fun pem(encodedPublicKey: ByteArray): String =
        "-----BEGIN PUBLIC KEY-----\n${Base64.getMimeEncoder(64, byteArrayOf('\n'.code.toByte())).encodeToString(encodedPublicKey)}\n-----END PUBLIC KEY-----"

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun derToRaw(der: ByteArray): ByteArray {
        var cursor = 2
        fun readInteger(): ByteArray {
            require(der[cursor++].toInt() == 2)
            val length = der[cursor++].toInt() and 0xff
            val value = der.copyOfRange(cursor, cursor + length).dropWhile { it == 0.toByte() }.toByteArray()
            cursor += length
            require(value.size <= 32)
            return ByteArray(32 - value.size) + value
        }
        return readInteger() + readInteger()
    }
}
