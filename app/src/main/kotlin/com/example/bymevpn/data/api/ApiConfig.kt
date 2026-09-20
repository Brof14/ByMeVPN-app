package com.example.bymevpn.data.api

import org.json.JSONObject

/**
 * High-level configuration constants for backend communication and security.
 */
object ApiConfig {
    /**
     * Backend API base URL. Production HTTPS API on port 8443.
     */
    const val BASE_URL = "https://api.parahin.space:8443/api/v1"

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
 * Standard API error model returned by backend.
 * Handles FastAPI formats:
 * - {"detail": {"error": {"code": "...", "message": "..."}}}
 * - {"detail": [{"loc": [...], "msg": "..."}]}
 * - {"detail": "..."}
 * - {"error": {"code": "...", "message": "..."}}
 */
data class ApiError(
    val code: String,
    override val message: String
) : Exception("[$code] $message") {
    companion object {
        fun fromJson(jsonStr: String): ApiError {
            return try {
                val root = JSONObject(jsonStr)

                // Check FastAPI {"detail": ...} format
                if (root.has("detail")) {
                    val detailObj = root.optJSONObject("detail")
                    if (detailObj != null) {
                        val nestedErr = detailObj.optJSONObject("error")
                        if (nestedErr != null) {
                            return ApiError(
                                code = nestedErr.optString("code", "API_ERROR"),
                                message = nestedErr.optString("message", "An error occurred")
                            )
                        }
                        return ApiError(
                            code = detailObj.optString("code", "API_ERROR"),
                            message = detailObj.optString("message", detailObj.toString())
                        )
                    }

                    val detailArray = root.optJSONArray("detail")
                    if (detailArray != null && detailArray.length() > 0) {
                        val firstItem = detailArray.optJSONObject(0)
                        val msg = firstItem?.optString("msg") ?: "Validation error"
                        return ApiError("VALIDATION_ERROR", msg)
                    }

                    val detailStr = root.optString("detail")
                    if (detailStr.isNotBlank()) {
                        return ApiError("API_ERROR", detailStr)
                    }
                }

                // Check standard {"error": ...} format
                if (root.has("error")) {
                    val errObj = root.optJSONObject("error")
                    if (errObj != null) {
                        return ApiError(
                            code = errObj.optString("code", "API_ERROR"),
                            message = errObj.optString("message", "An unexpected error occurred")
                        )
                    } else {
                        val errMsg = root.optString("error", "An unexpected error occurred")
                        return ApiError("API_ERROR", errMsg)
                    }
                }

                ApiError("API_ERROR", jsonStr)
            } catch (e: Exception) {
                ApiError("NETWORK_ERROR", jsonStr.ifBlank { "Network or parsing error" })
            }
        }
    }
}
