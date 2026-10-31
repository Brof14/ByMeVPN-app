package com.example.bymevpn.data.api

import android.content.Context
import android.util.Log
import com.example.bymevpn.data.storage.SecureTokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.UUID

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
        // Shared across instances to serialize refresh-token rotation.
        private val refreshMutex = Mutex()
    }

    private val storage = SecureTokenStorage.getInstance(context)

    // -------------------------------------------------------------
    // AUTHENTICATION ENDPOINTS
    // -------------------------------------------------------------

    private suspend fun processAuthPayload(json: JSONObject, auth: AuthResponse) {
        // New logins must not reuse another account's servers or pending payments.
        storage.clearTokens()
        storage.saveTokens(auth.access, auth.refresh)
        storage.saveCachedUser(auth.user)
        auth.subscription?.let { storage.saveCachedSubscription(it) }

        // Save immediately; AccountRepository refreshes nodes in the background.
        val subUrl = auth.subscription?.subscriptionUrl
            ?: json.optString("subscription_url").ifBlank {
                json.optJSONObject("subscription")?.optString("subscription_url") ?: ""
            }
        if (subUrl.isNotBlank()) {
            storage.saveSubscriptionUrl(subUrl)
        }

        // Extract and persist real server configurations if present
        val serversArray = json.optJSONArray("servers")
        if (serversArray != null && serversArray.length() > 0) {
            val serverList = mutableListOf<ServerNode>()
            for (i in 0 until serversArray.length()) {
                val sObj = serversArray.getJSONObject(i)
                val node = ApiJsonParsers.parseServers(JSONArray().apply { put(sObj) }).firstOrNull()
                val cfgObj = sObj.optJSONObject("config")
                if (node != null && cfgObj != null) {
                    val cfg = ApiJsonParsers.parseVpnSessionConfig(cfgObj)
                    storage.saveServerConfig(node.nodeCode, cfg)
                    serverList.add(node)
                }
            }
            if (serverList.isNotEmpty()) {
                com.example.bymevpn.vpn.VpnManager.getInstance(context).setServers(serverList)
            }
        }
    }

    suspend fun register(email: String, password: String, name: String): AuthResponse {
        val body = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("name", name.trim())
            put("install_id", storage.getInstallId(context))
        }
        val json = request("POST", "/auth/register", body, requiresAuth = false)
        val auth = ApiJsonParsers.parseAuthResponse(json)
        processAuthPayload(json, auth)
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
        processAuthPayload(json, auth)
        return auth
    }

    suspend fun loginGoogle(idToken: String): AuthResponse {
        val body = JSONObject().apply {
            put("id_token", idToken)
            put("install_id", storage.getInstallId(context))
        }
        val json = request("POST", "/auth/google", body, requiresAuth = false)
        val auth = ApiJsonParsers.parseAuthResponse(json)
        processAuthPayload(json, auth)
        return auth
    }

    suspend fun createTelegramSession(): TelegramSessionResponse {
        val json = request("POST", "/auth/telegram/create-session", JSONObject(), requiresAuth = false)
        return TelegramSessionResponse(
            sessionToken = json.optString("session_token"),
            botUrl = json.optString("bot_url")
        )
    }

    suspend fun checkTelegramSession(token: String): TelegramCheckResult {
        val json = request("GET", "/auth/telegram/check-session?token=${URLEncoder.encode(token, "UTF-8")}", null, requiresAuth = false)
        val status = json.optString("status", "pending")
        if (status == "authenticated") {
            val accessToken = json.optString("access_token").ifBlank { json.optString("access") }
            val refreshToken = json.optString("refresh_token").ifBlank { json.optString("refresh") }
            if (accessToken.isBlank() || refreshToken.isBlank()) {
                throw ApiError("INVALID_AUTH_RESPONSE", "Telegram authentication did not return both access and refresh tokens")
            }
            val user = ApiJsonParsers.parseUserProfile(json.optJSONObject("user") ?: JSONObject())
            val sub = json.optJSONObject("subscription")?.let { ApiJsonParsers.parseSubscriptionStatus(it) }
            val auth = AuthResponse(
                access = accessToken,
                refresh = refreshToken,
                user = user,
                subscription = sub
            )
            processAuthPayload(json, auth)
            return TelegramCheckResult(status = "authenticated", auth = auth)
        }
        return TelegramCheckResult(status = status, auth = null)
    }

    /** Revokes a previous session without changing the current local account. */
    suspend fun revokeSession(accessToken: String) {
        if (accessToken.isBlank()) return
        request("POST", "/auth/logout", JSONObject(), requiresAuth = false, authTokenOverride = accessToken)
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
        val subUrl = json.optString("subscription_url")
        if (subUrl.isNotBlank()) storage.saveSubscriptionUrl(subUrl)
        val array = json.optJSONArray("servers") ?: JSONArray()
        // No URLs or tokens here: only counts and presence flags are diagnostic.
        Log.d(TAG, "GET /servers: nodes=${array.length()}, subscriptionUrl=${if (subUrl.isBlank()) "absent" else "present"}")
        val parsed = ApiJsonParsers.parseServers(array)
        for (i in 0 until array.length()) {
            val sObj = array.getJSONObject(i)
            val code = sObj.optString("node_code")
            val cfgObj = sObj.optJSONObject("config")
            if (code.isNotBlank() && cfgObj != null) {
                val cfg = ApiJsonParsers.parseVpnSessionConfig(cfgObj)
                storage.saveServerConfig(code, cfg)
            }
        }
        if (subUrl.isNotBlank()) {
            val subManager = com.example.bymevpn.data.subscription.SubscriptionManager.getInstance(context)
            if (parsed.isEmpty()) {
                // The API has no nodes; the subscription URL is the only possible source.
                try {
                    subManager.applySubscriptionUrl(subUrl)
                } catch (e: Exception) {
                    Log.w(TAG, "Subscription URL from /servers was not applied: ${safeError(e)}")
                }
            } else {
                // Nodes are already available: never delay the server list for a subscription fetch.
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        subManager.applySubscriptionUrl(subUrl)
                    } catch (e: Exception) {
                        Log.w(TAG, "Background subscription refresh failed: ${safeError(e)}")
                    }
                }
            }
        }
        return parsed
    }

    suspend fun createVpnSession(nodeCode: String): VpnSessionConfig {
        val body = JSONObject().apply {
            put("node_code", nodeCode)
            put("install_id", storage.getInstallId(context))
        }
        val json = request("POST", "/vpn/session", body, requiresAuth = true)
        val config = ApiJsonParsers.parseVpnSessionConfig(json)
        if (config.server.isBlank() || config.uuid.isBlank()) {
            throw ApiError("INVALID_CONFIG", "Server returned incomplete VPN session configuration: server or uuid missing")
        }
        if (config.security.equals("reality", ignoreCase = true) && config.publicKey.isBlank()) {
            throw ApiError("INVALID_CONFIG", "Server returned incomplete VPN session configuration: missing public key for Reality")
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
    // IN-APP PAYMENTS (YooKassa Cards/SBP & CryptoBot)
    // -------------------------------------------------------------

    suspend fun createPayment(
        planCode: String,
        amountRub: Int,
        days: Int,
        devices: Int = 2,
        provider: String = "yookassa",
        requestId: String = UUID.randomUUID().toString()
    ): PaymentCreateResponse {
        val body = JSONObject().apply {
            put("plan_code", planCode)
            put("request_id", requestId)
            put("amount_rub", amountRub)
            put("days", days)
            put("devices", devices)
            put("provider", provider)
        }
        val json = request("POST", "/payment/create", body, requiresAuth = true)
        return PaymentCreateResponse(
            paymentUrl = json.optString("payment_url"),
            provider = json.optString("provider", provider),
            invoiceId = json.optString("invoice_id").ifEmpty { null }
        )
    }

    suspend fun checkPayment(invoiceId: String, provider: String): PaymentCheckResult {
        val encodedInvoiceId = java.net.URLEncoder.encode(invoiceId, "UTF-8")
        val encodedProvider = java.net.URLEncoder.encode(provider, "UTF-8")
        val json = request("GET", "/payment/check?invoice_id=$encodedInvoiceId&provider=$encodedProvider", null, requiresAuth = true)
        val status = json.optString("status", "none")
        val isActive = json.optBoolean("is_active", false)
        val subObj = json.optJSONObject("subscription")
        val sub = subObj?.let { ApiJsonParsers.parseSubscriptionStatus(it) }
        return PaymentCheckResult(
            status = status,
            isActive = isActive,
            subscription = sub
        )
    }

    // -------------------------------------------------------------
    // INTERNAL HTTP ENGINE WITH 401 TOKEN REFRESH
    // -------------------------------------------------------------

    private suspend fun request(
        method: String,
        endpoint: String,
        body: JSONObject?,
        requiresAuth: Boolean,
        isRetry: Boolean = false,
        authTokenOverride: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val fullUrl = "${ApiConfig.BASE_URL}$endpoint"
        if (!URL(fullUrl).protocol.equals("https", ignoreCase = true)) {
            throw ApiError("INSECURE_BACKEND", "Backend HTTPS is required before account requests can be sent")
        }

        val tokenAtRequest = if (requiresAuth) storage.getAccessToken() else null
        val connection = try {
            (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                val installId = storage.getInstallId(context)
                if (installId.isNotBlank()) {
                    setRequestProperty("X-HWID", installId)
                }
                setRequestProperty("X-Device-OS", "Android")
                setRequestProperty(
                    "X-Ver-OS",
                    android.os.Build.VERSION.RELEASE?.ifBlank { "unknown" } ?: "unknown"
                )
                setRequestProperty(
                    "X-Device-Model",
                    storage.getDeviceModel().ifBlank { "Android device" }
                )

                if (requiresAuth || authTokenOverride != null) {
                    val token = authTokenOverride ?: tokenAtRequest
                    if (!token.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }
            }
        } catch (e: Exception) {
            throw ApiError("NETWORK_ERROR", e.localizedMessage ?: "Failed to open connection")
        }

        try {
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
            // Another client may have already rotated the token while this request was in flight.
            val newerTokenAvailable = tokenAtRequest != storage.getAccessToken() && !storage.getAccessToken().isNullOrBlank()
            val refreshSuccess = newerTokenAvailable || refreshAccessToken()
            if (refreshSuccess) {
                return@withContext request(method, endpoint, body, requiresAuth, isRetry = true)
            } else {
                if (storage.getAccessToken() == tokenAtRequest) storage.clearTokens()
                throw ApiError("UNAUTHORIZED", "Session expired. Please log in again.")
            }
        }

        val responseString = try {
            val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            inputStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
        } catch (e: Exception) {
            if (responseCode in 200..299) throw ApiError("NETWORK_ERROR", "Failed to read backend response")
            else ""
        }

        if (responseCode !in 200..299) {
            throw if (responseString.isBlank()) ApiError("HTTP_$responseCode", "Backend returned HTTP $responseCode")
            else ApiError.fromJson(responseString)
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
        } finally {
            connection.disconnect()
        }
    }

    /** Keeps diagnostics useful without ever logging a subscription URL or a token. */
    private fun safeError(e: Exception): String {
        val message = e.message.orEmpty()
        return if (message.isBlank() || message.contains("://")) e.javaClass.simpleName
        else "${e.javaClass.simpleName}: ${message.take(160)}"
    }

    private suspend fun refreshAccessToken(): Boolean = refreshMutex.withLock {
        withContext(Dispatchers.IO) {
            val refreshToken = storage.getRefreshToken() ?: return@withContext false
            val fullUrl = "${ApiConfig.BASE_URL}/auth/refresh"
            if (!URL(fullUrl).protocol.equals("https", ignoreCase = true)) return@withContext false
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

                try {
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
                        synchronized(storage) {
                            if (storage.getRefreshToken() != refreshToken) false
                            else {
                                storage.saveTokens(newAccess, newRefresh)
                                true
                            }
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Refresh token error: ${e.message}")
                false
            }
        }
    }
}
