package com.example.bymevpn.data.api

import android.content.Context
import android.util.Log
import com.example.bymevpn.data.storage.SecureTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Production-ready HTTP client communicating directly with the ByMeVPN backend.
 * Implements:
 * - Automatic Authorization: Bearer <access_token>
 * - Automatic 401 handling with Refresh Token rotation and retry
 * - Error decoding according to backend contract: {"error": {"code": "...", "message": "..."}}
 */
class ByMeApiClient(private val context: Context) {

    companion object {
        private const val TAG = "ByMeApiClient"
        private const val TIMEOUT_MS = 15000
    }

    private val storage = SecureTokenStorage.getInstance(context)
    private val refreshMutex = Mutex()

    // -------------------------------------------------------------
    // AUTHENTICATION ENDPOINTS
    // -------------------------------------------------------------

    suspend fun register(email: String, password: String, name: String): AuthResponse {
        val body = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("name", name.trim())
            put("install_id", storage.getInstallId(context))
        }
        val json = request("POST", "/auth/register", body, requiresAuth = false)
        val auth = ApiJsonParsers.parseAuthResponse(json)
        storage.saveTokens(auth.access, auth.refresh)
        storage.saveCachedUser(auth.user)
        auth.subscription?.let { storage.saveCachedSubscription(it) }
        return auth
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val body = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("install_id", storage.getInstallId(context))
        }
        val json = request("POST", "/auth/login", body, requiresAuth = false)
        val auth = ApiJsonParsers.parseAuthResponse(json)
        storage.saveTokens(auth.access, auth.refresh)
        storage.saveCachedUser(auth.user)
        auth.subscription?.let { storage.saveCachedSubscription(it) }
        return auth
    }

    suspend fun loginGoogle(idToken: String): AuthResponse {
        val body = JSONObject().apply {
            put("id_token", idToken)
            put("install_id", storage.getInstallId(context))
        }
        val json = request("POST", "/auth/google", body, requiresAuth = false)
        val auth = ApiJsonParsers.parseAuthResponse(json)
        storage.saveTokens(auth.access, auth.refresh)
        storage.saveCachedUser(auth.user)
        auth.subscription?.let { storage.saveCachedSubscription(it) }
        return auth
    }

    suspend fun logout() {
        try {
            request("POST", "/auth/logout", JSONObject(), requiresAuth = true)
        } catch (e: Exception) {
            Log.w(TAG, "Logout request failed: ${e.message}")
        } finally {
            storage.clearTokens()
        }
    }

    suspend fun getMe(): UserProfile {
        val json = request("GET", "/me", null, requiresAuth = true)
        val user = ApiJsonParsers.parseUserProfile(json)
        storage.saveCachedUser(user)
        return user
    }

    suspend fun deleteMe() {
        request("DELETE", "/me", null, requiresAuth = true)
        storage.clearTokens()
    }

    // -------------------------------------------------------------
    // SUBSCRIPTION & TRIAL ENDPOINTS
    // -------------------------------------------------------------

    suspend fun getSubscriptionStatus(): SubscriptionStatus {
        val json = request("GET", "/subscription/status", null, requiresAuth = true)
        val sub = ApiJsonParsers.parseSubscriptionStatus(json)
        storage.saveCachedSubscription(sub)
        return sub
    }

    suspend fun activateTrial(): SubscriptionStatus {
        val body = JSONObject().apply {
            put("install_id", storage.getInstallId(context))
            put("device_model", storage.getDeviceModel())
        }
        val json = request("POST", "/subscription/trial", body, requiresAuth = true)
        val sub = ApiJsonParsers.parseSubscriptionStatus(json)
        storage.saveCachedSubscription(sub)
        return sub
    }

    suspend fun getPlans(): List<PlanItem> {
        val json = request("GET", "/plans", null, requiresAuth = false)
        val array = if (json.has("raw_array")) {
            json.getJSONArray("raw_array")
        } else if (json.has("plans")) {
            json.getJSONArray("plans")
        } else {
            try {
                val rawStr = json.optString("raw")
                if (rawStr.isNotBlank()) JSONArray(rawStr) else JSONArray()
            } catch (e: Exception) {
                JSONArray()
            }
        }

        val list = mutableListOf<PlanItem>()
        for (i in 0 until array.length()) {
            val p = array.getJSONObject(i)
            val code = p.optString("code", "")
            val name = p.optString("name", code)
            val priceRub = p.optInt("price_rub", 0)
            val price = if (priceRub > 0) "$priceRub ₽" else p.optString("price", "89 ₽")
            val days = p.optInt("days", 30)
            val period = when {
                days <= 30 -> "1 месяц"
                days <= 90 -> "3 месяца"
                days <= 180 -> "6 месяцев"
                days <= 365 -> "1 год"
                else -> "$days дней"
            }
            val discount = when (code) {
                "quarter" -> 15
                "half_year" -> 30
                "year" -> 45
                else -> p.optInt("discount_percent", 0)
            }
            list.add(
                PlanItem(
                    code = code,
                    name = name,
                    price = price,
                    period = period,
                    isPopular = code == "half_year" || code == "quarter" || p.optBoolean("is_popular", false),
                    discountPercent = discount
                )
            )
        }
        return list
    }

    // -------------------------------------------------------------
    // SERVERS & VPN SESSION
    // -------------------------------------------------------------

    suspend fun getServers(): List<ServerNode> {
        val json = request("GET", "/servers", null, requiresAuth = true)
        val array = json.optJSONArray("servers") ?: JSONArray()
        val parsed = ApiJsonParsers.parseServers(array)
        if (parsed.isNotEmpty()) {
            return parsed
        }
        throw ApiError("NO_SERVERS", "No active servers returned by backend")
    }

    suspend fun createVpnSession(nodeCode: String): VpnSessionConfig {
        val body = JSONObject().apply {
            put("node_code", nodeCode)
            put("install_id", storage.getInstallId(context))
        }
        val json = request("POST", "/vpn/session", body, requiresAuth = true)
        val config = ApiJsonParsers.parseVpnSessionConfig(json)
        if (config.server.isBlank() || config.uuid.isBlank() || config.publicKey.isBlank()) {
            throw ApiError("INVALID_CONFIG", "Server returned incomplete VPN session configuration")
        }
        return config
    }

    suspend fun deleteVpnSession() {
        try {
            request("DELETE", "/vpn/session", null, requiresAuth = true)
        } catch (e: Exception) {
            Log.w(TAG, "deleteVpnSession failed: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // DEVICES
    // -------------------------------------------------------------

    suspend fun getDevices(): List<DeviceItem> {
        val json = request("GET", "/devices", null, requiresAuth = true)
        val array = json.optJSONArray("devices") ?: JSONArray()
        return ApiJsonParsers.parseDevices(array, storage.getInstallId(context))
    }

    suspend fun deleteDevice(deviceId: String) {
        request("DELETE", "/devices/$deviceId", null, requiresAuth = true)
    }

    // -------------------------------------------------------------
    // INTERNAL HTTP ENGINE WITH 401 TOKEN REFRESH
    // -------------------------------------------------------------

    private suspend fun request(
        method: String,
        endpoint: String,
        body: JSONObject?,
        requiresAuth: Boolean,
        isRetry: Boolean = false
    ): JSONObject = withContext(Dispatchers.IO) {
        val fullUrl = "${ApiConfig.BASE_URL}$endpoint"

        val connection = try {
            (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")

                if (requiresAuth) {
                    val token = storage.getAccessToken()
                    if (!token.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }
            }
        } catch (e: Exception) {
            throw ApiError("NETWORK_ERROR", e.localizedMessage ?: "Failed to open connection")
        }

        if (body != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
            connection.doOutput = true
            try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }
            } catch (e: Exception) {
                throw ApiError("NETWORK_ERROR", e.localizedMessage ?: "Failed to send request")
            }
        }

        val responseCode = try {
            connection.responseCode
        } catch (e: Exception) {
            throw ApiError("NETWORK_ERROR", e.localizedMessage ?: "Network error connecting to backend")
        }

        // Handle 401 Unauthorized -> try refresh token once
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED && requiresAuth && !isRetry) {
            val refreshSuccess = refreshAccessToken()
            if (refreshSuccess) {
                return@withContext request(method, endpoint, body, requiresAuth, isRetry = true)
            } else {
                storage.clearTokens()
                throw ApiError("UNAUTHORIZED", "Session expired. Please log in again.")
            }
        }

        val inputStream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        val responseString = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

        if (responseCode !in 200..299) {
            throw ApiError.fromJson(responseString)
        }

        if (responseString.isBlank()) {
            JSONObject()
        } else {
            val trimmed = responseString.trim()
            if (trimmed.startsWith("[")) {
                JSONObject().apply {
                    put("raw_array", JSONArray(trimmed))
                    put("raw", trimmed)
                }
            } else {
                try {
                    JSONObject(trimmed)
                } catch (e: Exception) {
                    JSONObject().apply { put("raw", trimmed) }
                }
            }
        }
    }

    private suspend fun refreshAccessToken(): Boolean = refreshMutex.withLock {
        withContext(Dispatchers.IO) {
            val refreshToken = storage.getRefreshToken() ?: return@withContext false
            val fullUrl = "${ApiConfig.BASE_URL}/auth/refresh"
            try {
                val connection = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doInput = true
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                }

                val body = JSONObject().apply { put("refresh", refreshToken) }
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                    it.write(body.toString())
                    it.flush()
                }

                if (connection.responseCode in 200..299) {
                    val resp = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(resp)
                    val newAccess = json.optString("access", json.optString("access_token", ""))
                    val newRefresh = json.optString("refresh", json.optString("refresh_token", refreshToken))
                    if (newAccess.isNotBlank()) {
                        storage.saveTokens(newAccess, newRefresh)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Refresh token error: ${e.message}")
                false
            }
        }
    }
}
