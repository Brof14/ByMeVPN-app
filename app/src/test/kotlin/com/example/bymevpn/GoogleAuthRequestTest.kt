package com.example.bymevpn

import com.example.bymevpn.data.api.GoogleAuthRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthRequestTest {
    @Test
    fun challengeIsRequestedWithoutClientChosenNonce() {
        assertEquals("/auth/google/nonce", GoogleAuthRequest.CHALLENGE_PATH)
        assertFalse(GoogleAuthRequest.CHALLENGE_PATH.contains("?"))
        assertEquals(0, GoogleAuthRequest.challengeBody().length())
    }

    @Test
    fun idTokenIsSentWithServerChallengeInJsonBody() {
        val token = "opaque-google-id-token"
        val nonce = "server-issued-one-time-nonce"
        val body = GoogleAuthRequest.tokenBody(token, nonce)

        assertEquals(token, body.getString("id_token"))
        assertEquals(nonce, body.getString("nonce"))
        assertTrue(body.length() == 2)
    }
}
