package com.example.bymevpn.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * User account model representing local user profile,
 * subscription details synced with web account (bymevpn-site.duckdns.org),
 * and language preferences.
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
 * Local repository simulating future backend sync (MySQL / PostgreSQL / Supabase / Firebase).
 * When user logs in via Google or email, it checks if user exists in the database.
 * If not, it automatically creates the account so the user is registered.
 */
object AccountRepository {
    // In-memory mock database of registered users with subscription data
    private val databaseUsers = mutableMapOf<String, UserAccount>(
        "mama.nikfjdj@gmail.com" to UserAccount(
            email = "mama.nikfjdj@gmail.com",
            name = "Mama",
            isGoogle = true,
            hasSubscription = true,
            planName = "Pro Unlimited 30 Days",
            daysRemaining = 28,
            hoursRemaining = 14,
            expirationDate = "15.10.2026"
        )
    )

    private val _currentUser = MutableStateFlow<UserAccount?>(databaseUsers["mama.nikfjdj@gmail.com"])
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    /**
     * Authenticate or register user.
     * If user is found in database, returns existing account with subscription.
     * If user doesn't exist, registers a new account in database.
     */
    fun loginOrRegister(email: String, name: String = "", isGoogle: Boolean = false): UserAccount {
        val normalizedEmail = email.trim().lowercase()
        val existing = databaseUsers[normalizedEmail]
        if (existing != null) {
            _currentUser.value = existing
            return existing
        }

        // Auto-register new user in database
        val displayName = if (name.isNotBlank()) name else normalizedEmail.substringBefore("@")
        val newUser = UserAccount(
            email = normalizedEmail,
            name = displayName,
            isGoogle = isGoogle,
            hasSubscription = false, // Newly registered users buy subscription on website
            planName = "Free Trial",
            daysRemaining = 3,
            hoursRemaining = 0,
            expirationDate = "20.09.2026"
        )
        databaseUsers[normalizedEmail] = newUser
        _currentUser.value = newUser
        return newUser
    }

    fun logout() {
        _currentUser.value = null
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
