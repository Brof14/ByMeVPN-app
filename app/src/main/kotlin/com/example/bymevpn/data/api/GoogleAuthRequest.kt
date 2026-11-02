package com.example.bymevpn.data.api

import org.json.JSONObject

/** Wire contract for one-time, server-issued Google sign-in challenges. */
internal object GoogleAuthRequest {
    const val CHALLENGE_PATH = "/auth/google/nonce"

    fun challengeBody(): JSONObject = JSONObject()

    fun tokenBody(idToken: String, nonce: String): JSONObject = JSONObject().apply {
        put("id_token", idToken)
        put("nonce", nonce)
    }
}
