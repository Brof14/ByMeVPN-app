package com.example.bymevpn.data

import android.content.Context
import android.util.Log
import com.example.bymevpn.data.api.ApiError
import com.example.bymevpn.data.api.ByMeApiClient
import com.example.bymevpn.data.api.DeviceItem
import com.example.bymevpn.data.api.SubscriptionStatus
import com.example.bymevpn.data.api.UserProfile
import com.example.bymevpn.data.auth.GoogleAuthHelper
import com.example.bymevpn.data.auth.GoogleAuthResult
import com.example.bymevpn.data.storage.SecureTokenStorage
import com.example.bymevpn.data.subscription.SubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pure client-side AccountRepository coordinating user session and device lists.
 * Zero hardcoded data: values come directly from the backend API.
 */
object AccountRepository {
    private const val TAG = "AccountRepository"

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _userDevices = MutableStateFlow<List<DeviceItem>>(emptyList())
    val userDevices: StateFlow<List<DeviceItem>> = _userDevices.asStateFlow()

    private val _deviceListUnavailable = MutableStateFlow(false)
    val deviceListUnavailable: StateFlow<Boolean> = _deviceListUnavailable.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false

    private fun refreshAfterLogin(context: Context) {
        val appContext = context.applicationContext
        repoScope.launch { refreshDevices(appContext) }
        repoScope.launch {
            val subscriptionManager = SubscriptionManager.getInstance(appContext)
            val statusResult = subscriptionManager.refreshStatus(forceBackend = true)
            val status = statusResult.getOrNull()
            if (status?.isUsable == true) {
                com.example.bymevpn.vpn.VpnManager.getInstance(appContext).loadServers()
            }
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val storage = SecureTokenStorage.getInstance(context)
        val cachedUser = storage.getCachedUser()
        if (cachedUser != null) {
            _currentUser.value = cachedUser
        }

        // Network refresh is coordinated from MainActivity.onResume. Starting a
        // second independent refresh here duplicated profile/device/status calls
        // on every cold launch and could race the entitlement-gated server fetch.
    }

    suspend fun loginWithEmail(context: Context, email: String, pass: String): Result<UserProfile> {
        _isLoading.value = true
        return try {
            val trimmedEmail = email.trim()
            val api = ByMeApiClient(context)
            val auth = api.login(trimmedEmail, pass)
            _currentUser.value = auth.user
            refreshAfterLogin(context)
            Result.success(auth.user)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun registerWithEmail(context: Context, email: String, pass: String, name: String = ""): Result<UserProfile> {
        _isLoading.value = true
        return try {
            val api = ByMeApiClient(context)
            val displayName = name.ifBlank { email.substringBefore("@") }
            val auth = api.register(email, pass, displayName)
            _currentUser.value = auth.user
            refreshAfterLogin(context)
            Result.success(auth.user)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loginWithGoogle(context: Context): Result<UserProfile> {
        if (!com.example.bymevpn.data.api.ApiConfig.GOOGLE_AUTH_SERVER_VERIFIED) {
            return Result.failure(ApiError("GOOGLE_AUTH_UNAVAILABLE", "Google sign-in is temporarily unavailable"))
        }
        _isLoading.value = true
        var stage = "request_nonce"
        return try {
            val api = ByMeApiClient(context)
            val nonce = api.createGoogleAuthNonce()
            stage = "google_credential"
            val helper = GoogleAuthHelper(context)
            when (val authResult = helper.signInWithGoogle(nonce)) {
                is GoogleAuthResult.Success -> {
                    stage = "verify_with_server"
                    val auth = api.loginGoogle(authResult.idToken, nonce)
                    _currentUser.value = auth.user
                    refreshAfterLogin(context)
                    Result.success(auth.user)
                }
                is GoogleAuthResult.Error -> {
                    Log.w(TAG, "Google sign-in failed at $stage")
                    Result.failure(ApiError("GOOGLE_AUTH_FAILED", authResult.message))
                }
                is GoogleAuthResult.Cancelled -> {
                    Result.failure(ApiError("USER_CANCELLED", "Sign-in cancelled"))
                }
            }
        } catch (e: Exception) {
            val apiError = e as? ApiError
            Log.w(
                TAG,
                "Google sign-in failed at $stage (code=${apiError?.code ?: e.javaClass.simpleName}, status=${apiError?.httpStatusCode ?: "n/a"}, correlation=${apiError?.correlationId ?: "n/a"})"
            )
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createTelegramAuthSession(context: Context): Result<com.example.bymevpn.data.api.TelegramSessionResponse> {
        return try {
            val api = ByMeApiClient(context)
            val session = api.createTelegramSession()
            Result.success(session)
        } catch (e: Exception) {
            val apiError = e as? ApiError
            Log.w(
                TAG,
                "Telegram sign-in session creation failed (code=${apiError?.code ?: e.javaClass.simpleName}, status=${apiError?.httpStatusCode ?: "n/a"}, correlation=${apiError?.correlationId ?: "n/a"})"
            )
            Result.failure(e)
        }
    }

    suspend fun checkTelegramAuthSession(context: Context, token: String): Result<UserProfile?> {
        return try {
            val api = ByMeApiClient(context)
            val check = api.checkTelegramSession(token)
            if (check.status == "authenticated" && check.auth != null) {
                _currentUser.value = check.auth.user
                // Do not hold Telegram login on status and device calls. Those
                // are refreshed in the background, so a panel outage cannot
                // turn a completed one-time login exchange into a login error.
                refreshAfterLogin(context)
                Result.success(check.auth.user)
            } else if (check.status == "expired") {
                Result.failure(ApiError("SESSION_EXPIRED", "Telegram sign-in session expired", 401))
            } else {
                Result.success(null) // pending confirmation
            }
        } catch (e: Exception) {
            val apiError = e as? ApiError
            Log.w(
                TAG,
                "Telegram sign-in confirmation check failed (code=${apiError?.code ?: e.javaClass.simpleName}, status=${apiError?.httpStatusCode ?: "n/a"}, correlation=${apiError?.correlationId ?: "n/a"})"
            )
            Result.failure(e)
        }
    }

    suspend fun refreshProfile(context: Context) {
        try {
            val api = ByMeApiClient(context)
            val user = api.getMe()
            _currentUser.value = user
        } catch (e: Exception) {
            logRequestFailure("Failed to refresh profile", e)
            clearExpiredSession(context, e)
        }
    }

    suspend fun refreshDevices(context: Context) {
        val storage = SecureTokenStorage.getInstance(context)
        try {
            val api = ByMeApiClient(context)
            val devices = api.getDevices()
            _userDevices.value = devices
            _deviceListUnavailable.value = false
            storage.saveRegisteredDevices(devices)
            return
        } catch (e: Exception) {
            logRequestFailure("Failed to refresh devices from API", e)
            clearExpiredSession(context, e)
            _deviceListUnavailable.value = true
        }

        // Do not display old locally fabricated devices as a current server response.
        _userDevices.value = emptyList()
    }

    private fun clearExpiredSession(context: Context, error: Exception) {
        if (!AccountSessionPolicy.isExpired(error)) return

        // ByMeApiClient has already cleared persisted credentials after refresh
        // failed. Clear the in-memory identity as well, so the UI cannot keep
        // presenting a cached profile as an authenticated account.
        _currentUser.value = null
        _userDevices.value = emptyList()
        _deviceListUnavailable.value = false
        SubscriptionManager.getInstance(context.applicationContext).clear()
    }

    private fun logRequestFailure(action: String, error: Exception) {
        val apiError = error as? ApiError
        Log.w(
            TAG,
            "$action (code=${apiError?.code ?: error.javaClass.simpleName}, status=${apiError?.httpStatusCode ?: "n/a"}, correlation=${apiError?.correlationId ?: "n/a"})"
        )
    }

    suspend fun removeDevice(context: Context, deviceId: String): Result<Unit> {
        return try {
            val api = ByMeApiClient(context)
            api.deleteDevice(deviceId)
            val storage = SecureTokenStorage.getInstance(context)
            storage.removeRegisteredDevice(deviceId)
            _userDevices.value = storage.getRegisteredDevices()
            refreshDevices(context)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout(context: Context) {
        val appContext = context.applicationContext
        val storage = SecureTokenStorage.getInstance(appContext)
        val tokenToRevoke = storage.getAccessToken()
        // Revoke local access immediately; network logout must not race the next login.
        storage.clearTokens()
        SubscriptionManager.getInstance(appContext).clear()
        _currentUser.value = null
        _userDevices.value = emptyList()
        _deviceListUnavailable.value = false
        com.example.bymevpn.vpn.VpnManager.getInstance(appContext).disconnect()
        if (!tokenToRevoke.isNullOrBlank()) {
            repoScope.launch {
                try {
                    ByMeApiClient(appContext).revokeSession(tokenToRevoke)
                } catch (e: Exception) {
                    Log.w(TAG, "Remote logout request could not be completed")
                }
            }
        }
    }

    suspend fun deleteAccount(context: Context): Result<Unit> {
        return try {
            val api = ByMeApiClient(context)
            api.deleteMe()
            SubscriptionManager.getInstance(context).clear()
            _currentUser.value = null
            _userDevices.value = emptyList()
            _deviceListUnavailable.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createLocalProfile(context: Context, name: String) {
        val storage = SecureTokenStorage.getInstance(context)
        val profile = UserProfile(
            id = storage.getInstallId(context),
            email = "",
            name = name,
            isGoogle = false,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        )
        storage.saveCachedUser(profile)
        _currentUser.value = profile
    }
}
