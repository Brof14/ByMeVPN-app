package com.example.bymevpn.data.api

import org.json.JSONObject

/**
 * High-level configuration constants for backend communication and security.
 */
object ApiConfig {
    /**
     * Backend API base URL. Points to active ByMeVPN deployment.
     */
    const val BASE_URL = "https://bymevpn-site.duckdns.org/api/v1"

    /**
     * Google Web Client ID for Android Credential Manager OAuth.
     * Web Client ID from Google Cloud Console project gen-lang-client-0358610581.
     */
    const val GOOGLE_SERVER_CLIENT_ID = "78634768186-c5faqurbenf7a003st3oiap79h8qajjv.apps.googleusercontent.com"

    /**
     * Server public key in PEM format for ES256 JWT offline entitlement verification.
     */
    const val SERVER_ES256_PUBLIC_KEY = """-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEbymevpnServerPublicKeyPlaceholderPlaceholderPlaceholderPlaceholderPlaceholderPlaceholderPlaceholderPlaceholder0=
-----END PUBLIC KEY-----"""

    // 72-hour offline entitlement allowance (in milliseconds)
    const val MAX_OFFLINE_DURATION_MS = 72L * 60 * 60 * 1000
}

/**
 * Standard API error model returned by backend:
 * {"error": {"code": "...", "message": "..."}}
 */
data class ApiError(
    val code: String,
    override val message: String
) : Exception("[$code] $message") {
    companion object {
        fun fromJson(jsonStr: String): ApiError {
            return try {
                val root = JSONObject(jsonStr)
                if (root.has("error")) {
                    val errObj = root.optJSONObject("error")
                    if (errObj != null) {
                        ApiError(
                            code = errObj.optString("code", "UNKNOWN_ERROR"),
                            message = errObj.optString("message", "An unexpected error occurred")
                        )
                    } else {
                        val errMsg = root.optString("error", "An unexpected error occurred")
                        ApiError("API_ERROR", errMsg)
                    }
                } else {
                    ApiError("API_ERROR", jsonStr)
                }
            } catch (e: Exception) {
                ApiError("NETWORK_ERROR", jsonStr.ifBlank { "Network or parsing error" })
            }
        }
    }
}
