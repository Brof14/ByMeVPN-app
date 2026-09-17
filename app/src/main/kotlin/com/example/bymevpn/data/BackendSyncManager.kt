package com.example.bymevpn.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sync manager interfacing with the official web portal (bymevpn-site.duckdns.org).
 * Checks server availability and syncs account subscriptions.
 */
object BackendSyncManager {
    private const val TAG = "BackendSyncManager"
    private const val BASE_URL = "https://bymevpn-site.duckdns.org"

    /**
     * Checks if the official backend is reachable.
     */
    suspend fun pingBackend(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(BASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "HEAD"
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            code in 200..399
        } catch (e: Exception) {
            Log.d(TAG, "Backend unreachable or offline: ${e.message}")
            false
        }
    }
}
