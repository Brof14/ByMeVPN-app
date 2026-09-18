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
     * Configure your Google Cloud OAuth 2.0 Web Client ID here or inject via BuildConfig.
     */
    const val GOOGLE_SERVER_CLIENT_ID = "YOUR_GOOGLE_SERVER_CLIENT_ID.apps.googleusercontent.com"

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
