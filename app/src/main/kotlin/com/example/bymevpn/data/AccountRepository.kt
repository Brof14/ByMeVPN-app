package com.example.bymevpn.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * User account model representing persistent user profile and subscription details.
 */
data class UserAccount(
    val email: String,
    val name: String = "",
    val isGoogle: Boolean = false,
    val hasSubscription: Boolean = true,
    val planName: String = "ByMeVPN Pro Unlimited",
    val daysRemaining: Int = 28,
    val hoursRemaining: Int = 14,
    val expirationDate: String = "15.10.2026",
    val isAutoRenew: Boolean = true
)

/**
 * Real repository for User Accounts and Sessions.
 * Backed by persistent SQLite database (ByMeDatabaseHelper).
 */
object AccountRepository {
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _userDevices = MutableStateFlow<List<UserDevice>>(emptyList())
    val userDevices: StateFlow<List<UserDevice>> = _userDevices.asStateFlow()

    private var dbHelper: ByMeDatabaseHelper? = null
    private var appContext: Context? = null
    private val repoScope = CoroutineScope(Dispatchers.IO)

    /**
     * Initializes repository with context and loads existing persistent session.
     */
    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        val db = ByMeDatabaseHelper.getInstance(appCtx)
        dbHelper = db

        // Restore active session from SQLite DB
        val activeUser = db.getActiveSessionUser()
        if (activeUser != null) {
            _currentUser.value = activeUser
            refreshDevices(activeUser.email)
        } else {
            // Default user seed if session is fresh
            val defaultUser = db.findUserByEmail("mama.nikfjdj@gmail.com")
            if (defaultUser != null) {
                _currentUser.value = defaultUser
                refreshDevices(defaultUser.email)
            }
        }

        // Background sync attempt with remote site
        repoScope.launch {
            BackendSyncManager.pingBackend()
        }
    }

    private fun getDb(context: Context? = null): ByMeDatabaseHelper? {
        if (dbHelper != null) return dbHelper
        if (context != null) {
            dbHelper = ByMeDatabaseHelper.getInstance(context)
            return dbHelper
        }
        return null
    }

    fun refreshDevices(email: String) {
        val db = dbHelper ?: return
        _userDevices.value = db.getUserDevices(email)
    }

    /**
     * Authenticate with email & password.
     */
    fun loginWithEmail(context: Context, email: String, password: String): Pair<UserAccount?, String?> {
        val db = getDb(context) ?: return Pair(null, "Database unavailable")
        val (user, error) = db.loginWithEmail(email, password)
        if (user != null) {
            _currentUser.value = user
            refreshDevices(user.email)
        }
        return Pair(user, error)
    }

    /**
     * Register new user with email & password.
     */
    fun registerWithEmail(context: Context, email: String, password: String, name: String = ""): Pair<UserAccount?, String?> {
        val db = getDb(context) ?: return Pair(null, "Database unavailable")
        val (user, error) = db.registerUser(email, password, name, isGoogle = false)
        if (user != null) {
            _currentUser.value = user
            refreshDevices(user.email)
        }
        return Pair(user, error)
    }

    /**
     * Authenticate via Google.
     */
    fun loginWithGoogle(context: Context, email: String, name: String): UserAccount {
        val db = getDb(context) ?: ByMeDatabaseHelper.getInstance(context)
        val user = db.loginWithGoogle(email, name)
        _currentUser.value = user
        refreshDevices(user.email)
        return user
    }

    /**
     * Backward-compatible login or register helper.
     */
    fun loginOrRegister(email: String, name: String = "", isGoogle: Boolean = false, context: Context? = null): UserAccount {
        val db = getDb(context)
        if (db != null) {
            val existing = db.findUserByEmail(email)
            if (existing != null) {
                db.saveSession(existing.email)
                _currentUser.value = existing
                refreshDevices(existing.email)
                return existing
            }
            if (isGoogle) {
                val ctx = context ?: appContext
                return if (ctx != null) {
                    loginWithGoogle(ctx, email, name)
                } else {
                    val fallback = UserAccount(email = email, name = name, isGoogle = true)
                    _currentUser.value = fallback
                    fallback
                }
            } else {
                val (user, _) = db.registerUser(email, "ByMePass2026!", name, isGoogle = false)
                val finalUser = user ?: UserAccount(email = email, name = name)
                _currentUser.value = finalUser
                refreshDevices(finalUser.email)
                return finalUser
            }
        }

        // In-memory fallback
        val user = UserAccount(email = email, name = name, isGoogle = isGoogle)
        _currentUser.value = user
        return user
    }

    /**
     * Log out: clears session in SQLite DB and resets in-memory user.
     */
    fun logout(context: Context? = null) {
        val db = getDb(context)
        db?.clearSession()
        _currentUser.value = null
        _userDevices.value = emptyList()
    }

    fun updateUserSubscription(days: Int, plan: String) {
        _currentUser.update { current ->
            current?.copy(
                hasSubscription = true,
                daysRemaining = days,
                planName = plan
            )
        }
    }
}
