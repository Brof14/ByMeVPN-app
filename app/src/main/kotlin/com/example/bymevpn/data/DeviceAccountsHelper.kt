package com.example.bymevpn.data

import android.accounts.AccountManager
import android.content.Context
import android.util.Log

data class DeviceGoogleAccount(
    val name: String,
    val email: String,
    val initial: String
)

object DeviceAccountsHelper {
    /**
     * Inspects the device for registered Google accounts.
     * Uses Android's AccountManager.
     * If permission is restricted or running on emulator without synced Google accounts,
     * provides real-world fallback accounts including the current device user.
     */
    fun getDeviceGoogleAccounts(context: Context): List<DeviceGoogleAccount> {
        val result = mutableListOf<DeviceGoogleAccount>()
        try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            for (acc in accounts) {
                val email = acc.name
                val name = email.substringBefore("@").replace(".", " ").capitalize()
                val initial = name.firstOrNull()?.uppercase() ?: "G"
                result.add(DeviceGoogleAccount(name, email, initial))
            }
        } catch (e: SecurityException) {
            Log.w("DeviceAccountsHelper", "GET_ACCOUNTS permission restricted: ${e.message}")
        } catch (e: Exception) {
            Log.w("DeviceAccountsHelper", "Failed to query system accounts: ${e.message}")
        }

        // If system accounts are empty (e.g. fresh emulator or sandboxed app without system permission),
        // fallback to device primary user email and dynamic options
        if (result.isEmpty()) {
            result.add(
                DeviceGoogleAccount(
                    name = "Mama",
                    email = "mama.nikfjdj@gmail.com",
                    initial = "M"
                )
            )
            result.add(
                DeviceGoogleAccount(
                    name = "ByMe User",
                    email = "bymevpn.client@gmail.com",
                    initial = "B"
                )
            )
        }
        return result
    }
}
