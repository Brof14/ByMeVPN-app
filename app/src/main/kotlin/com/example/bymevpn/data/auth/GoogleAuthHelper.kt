package com.example.bymevpn.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.bymevpn.data.LocaleManager
import com.example.bymevpn.data.api.ApiConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class GoogleAuthResult {
    data class Success(val idToken: String, val email: String, val displayName: String?) : GoogleAuthResult()
    data class Error(val message: String, val code: String = "GOOGLE_AUTH_FAILED") : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
}

internal object GoogleCredentialErrorClassifier {
    fun classify(
        type: String?,
        message: String?,
        isUnsupportedException: Boolean = false
    ): String {
        if (isUnsupportedException) return "GOOGLE_CREDENTIAL_UNAVAILABLE"
        val msg = message.orEmpty()
        val t = type.orEmpty()
        return when {
            t.contains("CANCELLATION", ignoreCase = true) ||
                msg.contains("[16]") ||
                msg.contains("CANCELED", ignoreCase = true) ||
                msg.contains("CANCELLED", ignoreCase = true) -> "USER_CANCELLED"

            msg.contains("[10]") ||
                msg.contains("DEVELOPER_ERROR", ignoreCase = true) ||
                msg.contains("SHA-1", ignoreCase = true) ||
                msg.contains("10:") -> "GOOGLE_AUTH_CONFIG_ERROR"

            msg.contains("[7]") ||
                msg.contains("NETWORK_ERROR", ignoreCase = true) -> "NETWORK_ERROR"

            t.contains("INTERRUPTED", ignoreCase = true) ||
                msg.contains("[8]") ||
                msg.contains("INTERNAL_ERROR", ignoreCase = true) -> "GOOGLE_AUTH_INTERRUPTED"

            t.contains("NO_CREDENTIAL", ignoreCase = true) ||
                msg.contains("NoCredential", ignoreCase = true) ||
                msg.contains("no credentials available", ignoreCase = true) -> "NO_GOOGLE_ACCOUNT"

            t.contains("PROVIDER_CONFIGURATION", ignoreCase = true) -> "GOOGLE_CREDENTIAL_UNAVAILABLE"

            else -> "GOOGLE_AUTH_FAILED"
        }
    }
}

/**
 * Standard Android Credential Manager integration for real Google ID-Token authentication.
 */
class GoogleAuthHelper(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthHelper"

        private tailrec fun findActivity(context: Context): Activity? = when (context) {
            is Activity -> context
            is ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }

        private fun isPlayServicesPresentAndEnabled(context: Context): Boolean = runCatching {
            val info = context.packageManager.getApplicationInfo("com.google.android.gms", 0)
            info.enabled
        }.getOrDefault(false)
    }

    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(serverNonce: String): GoogleAuthResult = withContext(Dispatchers.Main) {
        if (ApiConfig.GOOGLE_SERVER_CLIENT_ID.startsWith("YOUR_GOOGLE_SERVER_CLIENT_ID")) {
            return@withContext GoogleAuthResult.Error(
                message = "Google Sign-In requires configuring a valid Google Web Client ID in ApiConfig.GOOGLE_SERVER_CLIENT_ID",
                code = "GOOGLE_CREDENTIAL_UNAVAILABLE"
            )
        }
        if (serverNonce.isBlank()) {
            return@withContext GoogleAuthResult.Error(
                message = "Google sign-in challenge is unavailable",
                code = "GOOGLE_AUTH_UNAVAILABLE"
            )
        }
        if (!isPlayServicesPresentAndEnabled(context)) {
            val isRu = LocaleManager.isRussian()
            return@withContext GoogleAuthResult.Error(
                message = if (isRu) {
                    "Сервисы Google Play недоступны на этом устройстве."
                } else {
                    "Google Play Services are unavailable on this device."
                },
                code = "GOOGLE_CREDENTIAL_UNAVAILABLE"
            )
        }
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(ApiConfig.GOOGLE_SERVER_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .setNonce(serverNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val uiContext: Context = findActivity(context) ?: context
            val result = credentialManager.getCredential(
                request = request,
                context = uiContext
            )

            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleAuthResult.Success(
                    idToken = googleIdTokenCredential.idToken,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName
                )
            } else {
                GoogleAuthResult.Error("Unsupported credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google sign-in cancelled by user")
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialUnsupportedException) {
            Log.w(TAG, "Credential Manager unsupported on device")
            GoogleAuthResult.Error(
                message = e.localizedMessage ?: "Google credential manager unsupported",
                code = "GOOGLE_CREDENTIAL_UNAVAILABLE"
            )
        } catch (e: NoCredentialException) {
            val classified = GoogleCredentialErrorClassifier.classify(e.type, e.message)
            val code = if (classified == "GOOGLE_AUTH_FAILED") "NO_GOOGLE_ACCOUNT" else classified
            Log.w(TAG, "No Google credentials available on device (classified=$code)")
            val isRu = LocaleManager.isRussian()
            GoogleAuthResult.Error(
                message = if (isRu) {
                    "На устройстве не найден аккаунт Google. Добавьте аккаунт в настройках или войдите по почте."
                } else {
                    "No Google account found on this device. Add one in Settings or sign in with email."
                },
                code = code
            )
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Google Credential Provider unavailable: ${e.type}")
            GoogleAuthResult.Error(
                message = e.localizedMessage ?: "Google credential provider unavailable",
                code = "GOOGLE_CREDENTIAL_UNAVAILABLE"
            )
        } catch (e: GetCredentialInterruptedException) {
            Log.w(TAG, "Google sign-in interrupted")
            GoogleAuthResult.Error(
                message = e.localizedMessage ?: "Google sign-in interrupted",
                code = "GOOGLE_AUTH_INTERRUPTED"
            )
        } catch (e: GetCredentialException) {
            val classifiedCode = GoogleCredentialErrorClassifier.classify(e.type, e.message)
            Log.e(TAG, "Google Credential Manager error: ${e.javaClass.simpleName} (${e.type}) -> $classifiedCode")
            if (classifiedCode == "USER_CANCELLED") {
                GoogleAuthResult.Cancelled
            } else {
                GoogleAuthResult.Error(e.localizedMessage ?: "Google sign-in failed", code = classifiedCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Google sign in error: ${e.javaClass.simpleName}")
            GoogleAuthResult.Error(e.localizedMessage ?: "Failed to initialize Google Sign In")
        }
    }
}
