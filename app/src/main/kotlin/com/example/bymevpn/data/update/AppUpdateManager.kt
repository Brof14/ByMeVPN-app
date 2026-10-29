package com.example.bymevpn.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val GITHUB_REPO = "Brof14/ByMeVPN-app"
    private const val CURRENT_VERSION = "1.0.6"

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    suspend fun checkForUpdates(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ByMeVPN-AppUpdate")
            }

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val root = JSONObject(jsonStr)
                val tagName = root.optString("tag_name", "").removePrefix("v").trim()
                val body = root.optString("body", "")

                // Find APK asset
                var apkUrl = "https://github.com/$GITHUB_REPO/releases/latest/download/ByMeVPN.apk"
                val assets = root.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name")
                        if (name.endsWith(".apk") && !name.contains("debug")) {
                            apkUrl = asset.optString("browser_download_url", apkUrl)
                            break
                        }
                    }
                }

                if (isVersionNewer(tagName, CURRENT_VERSION)) {
                    val info = AppUpdateInfo(
                        latestVersion = tagName,
                        currentVersion = CURRENT_VERSION,
                        downloadUrl = apkUrl,
                        releaseNotes = body
                    )
                    _updateInfo.value = info
                    return@withContext info
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed: ${e.message}")
        }
        null
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank() || current.isBlank()) return false
        val lParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(lParts.size, cParts.size)) {
            val l = lParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun openUpdateDownload(context: Context, downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open update URL: ${e.message}")
        }
    }
}
