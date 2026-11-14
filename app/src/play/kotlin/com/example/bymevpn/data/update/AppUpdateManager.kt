package com.example.bymevpn.data.update

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppUpdateInfo(
    val latestVersion: String,
    val versionCode: Long,
    val currentVersion: String,
    val downloadUrl: String,
    val sha256: String,
    val releaseNotes: String
)

/** Play distributes updates itself; this variant cannot download or install APKs. */
object AppUpdateManager {
    private val noUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo = noUpdate.asStateFlow()

    suspend fun checkForUpdates(context: Context): AppUpdateInfo? = null

    suspend fun downloadAndInstall(context: Context, update: AppUpdateInfo): Nothing =
        error("Updates for this build are managed by Google Play")
}
