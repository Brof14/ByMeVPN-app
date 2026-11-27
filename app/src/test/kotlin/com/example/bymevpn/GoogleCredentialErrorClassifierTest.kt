package com.example.bymevpn

import com.example.bymevpn.data.auth.GoogleCredentialErrorClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleCredentialErrorClassifierTest {

    @Test
    fun classifiesDeveloperConfigAndSha1Errors() {
        assertEquals(
            "GOOGLE_AUTH_CONFIG_ERROR",
            GoogleCredentialErrorClassifier.classify(
                type = "android.credentials.GetCredentialException.TYPE_UNKNOWN",
                message = "Caller not whitelisted: [10] DEVELOPER_ERROR"
            )
        )
        assertEquals(
            "GOOGLE_AUTH_CONFIG_ERROR",
            GoogleCredentialErrorClassifier.classify(
                type = "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL",
                message = "[28444] Developer console is not set up correctly."
            )
        )
    }

    @Test
    fun classifiesCancellationNetworkInterruptedAndUnsupported() {
        assertEquals(
            "USER_CANCELLED",
            GoogleCredentialErrorClassifier.classify(
                type = "android.credentials.GetCredentialException.TYPE_USER_CANCELED",
                message = "[16] Account reauth failed."
            )
        )
        assertEquals(
            "NETWORK_ERROR",
            GoogleCredentialErrorClassifier.classify(
                type = "android.credentials.GetCredentialException.TYPE_UNKNOWN",
                message = "[7] NETWORK_ERROR while fetching token"
            )
        )
        assertEquals(
            "GOOGLE_AUTH_INTERRUPTED",
            GoogleCredentialErrorClassifier.classify(
                type = "android.credentials.GetCredentialException.TYPE_INTERRUPTED",
                message = "[8] INTERNAL_ERROR"
            )
        )
        assertEquals(
            "GOOGLE_CREDENTIAL_UNAVAILABLE",
            GoogleCredentialErrorClassifier.classify(
                type = "anything",
                message = "unsupported",
                isUnsupportedException = true
            )
        )
        assertEquals(
            "NO_GOOGLE_ACCOUNT",
            GoogleCredentialErrorClassifier.classify(
                type = "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL",
                message = "No credentials available"
            )
        )
    }
}
