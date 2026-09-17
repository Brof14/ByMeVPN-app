package com.example.bymevpn.data.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.bymevpn.data.api.ApiConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

sealed class GoogleAuthResult {
    data class Success(val idToken: String, val email: String, val displayName: String?) : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
}

/**
 * Standard Android Credential Manager integration for real Google ID-Token authentication.
 */
class GoogleAuthHelper(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthHelper"
    }

    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): GoogleAuthResult = withContext(Dispatchers.Main) {
        if (ApiConfig.GOOGLE_SERVER_CLIENT_ID.startsWith("YOUR_GOOGLE_SERVER_CLIENT_ID")) {
            return@withContext GoogleAuthResult.Error(
                "Google Sign-In requires configuring a valid Google Web Client ID in ApiConfig.GOOGLE_SERVER_CLIENT_ID"
            )
        }
        try {
            // Generate a cryptographic nonce for replay protection
            val nonceBytes = ByteArray(16)
            SecureRandom().nextBytes(nonceBytes)
            val nonce = Base64.encodeToString(nonceBytes, Base64.NO_WRAP or Base64.URL_SAFE)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(ApiConfig.GOOGLE_SERVER_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .setNonce(nonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
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
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Credential Manager error: ${e.message}", e)
            GoogleAuthResult.Error(e.localizedMessage ?: "Google sign-in failed")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Google sign in error: ${e.message}", e)
            GoogleAuthResult.Error(e.localizedMessage ?: "Failed to initialize Google Sign In")
        }
    }
}
