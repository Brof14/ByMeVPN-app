package com.example.bymevpn.data

import android.content.Context
import android.os.SystemClock
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private data class GoogleNonceTicket(
        val createdAtElapsedMs: Long,
        val request: Deferred<String>
    )
    private val googleNonceMutex = Mutex()
    private var pendingGoogleNonce: GoogleNonceTicket? = null
    private var googleSignInInProgress = false
    private const val GOOGLE_NONCE_MAX_AGE_MS = 240_000L

    @Volatile
    private var isInitializedFlag = false

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

    /**
     * Starts the one-time Google challenge request while the auth screen is idle.
     * The nonce is held for at most four minutes, consumed once, and never persisted.
     */
    fun prefetchGoogleSignInNonce(context: Context) {
        val appContext = context.applicationContext
        repoScope.launch {
            googleNonceMutex.withLock {
                if (googleSignInInProgress) return@withLock
                val now = SystemClock.elapsedRealtime()
                val current = pendingGoogleNonce
                if (current != null &&
                    now - current.createdAtElapsedMs in 0 until GOOGLE_NONCE_MAX_AGE_MS &&
                    !current.request.isCancelled
                ) return@withLock

                current?.request?.cancel()
                val request = repoScope.async {
                    ByMeApiClient(appContext).createGoogleAuthNonce()
                }
                pendingGoogleNonce = GoogleNonceTicket(now, request)
                request.invokeOnCompletion { cause ->
                    if (cause != null) {
                        repoScope.launch {
                            googleNonceMutex.withLock {
                                if (pendingGoogleNonce?.request === request) {
                                    pendingGoogleNonce = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun takeGoogleSignInNonce(context: Context): String {
        val appContext = context.applicationContext
        val request = googleNonceMutex.withLock {
            googleSignInInProgress = true
            val now = SystemClock.elapsedRealtime()
            val current = pendingGoogleNonce
            pendingGoogleNonce = null
            if (current != null &&
                now - current.createdAtElapsedMs in 0 until GOOGLE_NONCE_MAX_AGE_MS &&
                !current.request.isCancelled
            ) {
                current.request
            } else {
                current?.request?.cancel()
                repoScope.async {
                    ByMeApiClient(appContext).createGoogleAuthNonce()
                }
            }
        }
        return try {
            request.await()
        } catch (error: Exception) {
            googleNonceMutex.withLock { googleSignInInProgress = false }
            throw error
        }
    }

    private suspend fun finishGoogleSignInNonce() {
        googleNonceMutex.withLock { googleSignInInProgress = false }
    }

    fun init(context: Context) {
        if (isInitializedFlag) return
        isInitializedFlag = true

        val storage = SecureTokenStorage.getInstance(context)
        val cachedUser = storage.getCachedUser()
        if (cachedUser != null) {
            _currentUser.value = cachedUser
        }
        _isInitialized.value = true

        // Network refresh is coordinated from MainActivity.onResume. Starting a
        // second independent refresh here duplicated profile/device/status calls
        // on every cold launch and could race the entitlement-gated server fetch.
    }

    suspend fun initAsync(context: Context) {
        if (_isInitialized.value) return
        val appContext = context.applicationContext
        val cachedUser = withContext(Dispatchers.IO) {
            coroutineScope {
                val userDeferred = async { SecureTokenStorage.getInstance(appContext).getCachedUser() }
                val langDeferred = async {
                    runCatching {
                        com.example.bymevpn.data.settings.SettingsRepository.getInstance(appContext)
                            .settingsFlow.first().language
                    }.getOrNull()
                }
                val subWarm = async { runCatching { SubscriptionManager.getInstance(appContext) } }
                val user = userDeferred.await()
                langDeferred.await()?.let { code ->
                    LocaleManager.setLanguage(AppLanguage.fromCode(code))
                }
                subWarm.await()
                if (user != null) {
                    runCatching { com.example.bymevpn.vpn.VpnManager.getInstance(appContext) }
                }
                user
            }
        }
        if (cachedUser != null) {
            _currentUser.value = cachedUser
        }
        isInitializedFlag = true
        _isInitialized.value = true
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
            val nonce = takeGoogleSignInNonce(context)
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
                    Log.w(TAG, "Google sign-in failed at $stage (code=${authResult.code})")
                    Result.failure(ApiError(authResult.code, authResult.message))
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
            finishGoogleSignInNonce()
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
        val storage = SecureTokenStorage.getInstance(context)
        val requestedSessionId = storage.getAccountSessionId()
        try {
            val api = ByMeApiClient(context)
            val user = api.getMe()
            storage.withCurrentAccountSession(requestedSessionId) {
                _currentUser.value = user
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (storage.getAccountSessionId() != requestedSessionId) return
            logRequestFailure("Failed to refresh profile", e)
            clearExpiredSession(context, e)
        }
    }

    suspend fun refreshDevices(context: Context) {
        val storage = SecureTokenStorage.getInstance(context)
        val requestedSessionId = storage.getAccountSessionId()
        try {
            val api = ByMeApiClient(context)
            val devices = api.getDevices()
            val saved = storage.withCurrentAccountSession(requestedSessionId) {
                _userDevices.value = devices
                _deviceListUnavailable.value = false
                storage.saveRegisteredDevices(devices)
                true
            }
            if (saved != true) return
            return
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (storage.getAccountSessionId() != requestedSessionId) return
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
            com.example.bymevpn.vpn.VpnManager.getInstance(context.applicationContext).disconnect()
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
