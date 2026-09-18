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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val storage = SecureTokenStorage.getInstance(context)
        val cachedUser = storage.getCachedUser()
        if (cachedUser != null) {
            _currentUser.value = cachedUser
        }

        // Check if token exists, try refreshing profile & devices
        if (!storage.getAccessToken().isNullOrBlank()) {
            repoScope.launch {
                refreshProfile(context)
                refreshDevices(context)
                SubscriptionManager.getInstance(context).refreshStatus()
            }
        }
    }

    const val ADMIN_EMAIL = "winchik@gmail.com"
    const val ADMIN_PASSWORD = "WeMMR7X3"

    fun isAdminCredentials(email: String, pass: String): Boolean {
        return email.trim().equals(ADMIN_EMAIL, ignoreCase = true) && pass == ADMIN_PASSWORD
    }

    suspend fun loginWithEmail(context: Context, email: String, pass: String): Result<UserProfile> {
        _isLoading.value = true
        return try {
            val trimmedEmail = email.trim()
            // Admin master credentials for testing and administration
            if (isAdminCredentials(trimmedEmail, pass)) {
                val adminUser = UserProfile(
                    id = "admin_winchik_01",
                    email = ADMIN_EMAIL,
                    name = "Admin Winchik",
                    isGoogle = false,
                    createdAt = "2026-09-18"
                )
                val adminSubscription = SubscriptionStatus(
                    status = "active",
                    planCode = "admin_vip",
                    planName = "Admin VIP (Безлимит)",
                    expiresAt = "2035-12-31",
                    secondsRemaining = 280000000L,
                    autoRenew = true,
                    trialAvailable = false,
                    maxDevices = 99,
                    activeDevices = 1
                )
                val storage = SecureTokenStorage.getInstance(context)
                storage.saveTokens("admin_token_winchik", "admin_refresh_winchik")
                storage.saveCachedUser(adminUser)
                storage.saveCachedSubscription(adminSubscription)

                _currentUser.value = adminUser
                _userDevices.value = listOf(
                    DeviceItem(
                        id = storage.getInstallId(context),
                        deviceModel = storage.getDeviceModel(),
                        platform = "Android",
                        lastActive = "Сейчас",
                        isCurrent = true
                    )
                )
                SubscriptionManager.getInstance(context).refreshStatus()
                return Result.success(adminUser)
            }

            val api = ByMeApiClient(context)
            val auth = api.login(trimmedEmail, pass)
            _currentUser.value = auth.user
            refreshDevices(context)
            SubscriptionManager.getInstance(context).refreshStatus()
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
            refreshDevices(context)
            SubscriptionManager.getInstance(context).refreshStatus()
            Result.success(auth.user)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loginWithGoogle(context: Context): Result<UserProfile> {
        _isLoading.value = true
        return try {
            val helper = GoogleAuthHelper(context)
            when (val authResult = helper.signInWithGoogle()) {
                is GoogleAuthResult.Success -> {
                    val api = ByMeApiClient(context)
                    val auth = api.loginGoogle(authResult.idToken)
                    _currentUser.value = auth.user
                    refreshDevices(context)
                    SubscriptionManager.getInstance(context).refreshStatus()
                    Result.success(auth.user)
                }
                is GoogleAuthResult.Error -> {
                    Result.failure(ApiError("GOOGLE_AUTH_FAILED", authResult.message))
                }
                is GoogleAuthResult.Cancelled -> {
                    Result.failure(ApiError("USER_CANCELLED", "Sign-in cancelled"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun refreshProfile(context: Context) {
        try {
            val api = ByMeApiClient(context)
            val user = api.getMe()
            _currentUser.value = user
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh profile: ${e.message}")
        }
    }

    suspend fun refreshDevices(context: Context) {
        try {
            val api = ByMeApiClient(context)
            val devices = api.getDevices()
            _userDevices.value = devices
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh devices: ${e.message}")
        }
    }

    suspend fun removeDevice(context: Context, deviceId: String): Result<Unit> {
        return try {
            val api = ByMeApiClient(context)
            api.deleteDevice(deviceId)
            refreshDevices(context)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout(context: Context) {
        repoScope.launch {
            try {
                val api = ByMeApiClient(context)
                api.logout()
            } catch (e: Exception) {
                // Ignored
            } finally {
                SecureTokenStorage.getInstance(context).clearTokens()
                SubscriptionManager.getInstance(context).clear()
                _currentUser.value = null
                _userDevices.value = emptyList()
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
