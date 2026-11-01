package com.example.bymevpn.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.security.MessageDigest

data class AppUpdateInfo(
    val latestVersion: String,
    val versionCode: Long,
    val currentVersion: String,
    val downloadUrl: String,
    val sha256: String,
    val releaseNotes: String
)

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val UPDATE_ORIGIN = "https://bymevpn.duckdns.org"
    private const val MANIFEST_URL = "$UPDATE_ORIGIN/app/update.json"
    private const val MAX_APK_BYTES = 200L * 1024L * 1024L

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    suspend fun checkForUpdates(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            _updateInfo.value = null
            return@withContext null
        }
        var connection: HttpURLConnection? = null
        try {
            val activeConnection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ByMeVPN-Android")
            }
            connection = activeConnection
            if (activeConnection.responseCode !in 200..299) {
                _updateInfo.value = null
                return@withContext null
            }
            require(activeConnection.contentLengthLong in 0..32_768L) { "Update manifest is too large" }
            val manifestText = activeConnection.inputStream.bufferedReader().use { it.readText() }
            val current = currentAppVersion(context)
            val update = parseUpdateManifest(manifestText, current.first, current.second, "bymevpn.duckdns.org")
            _updateInfo.value = update
            update
        } catch (e: Exception) {
            // Network and parsing errors are expected on offline launches; never log URLs or response bodies.
            Log.w(TAG, "Update check failed: ${e.javaClass.simpleName}")
            _updateInfo.value = null
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun downloadAndInstall(context: Context, update: AppUpdateInfo) = withContext(Dispatchers.IO) {
        require(update.downloadUrl.isTrustedUpdateUrl()) { "Update link is not trusted" }
        require(update.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Update checksum is invalid" }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val temporaryApk = File(updatesDir, "bymevpn-update.apk.part")
        val apk = File(updatesDir, "bymevpn-update.apk")
        val connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
            connectTimeout = 15000
            readTimeout = 30000
            setRequestProperty("User-Agent", "ByMeVPN-Android")
        }

        try {
            require(connection.responseCode in 200..299) { "Could not download the update" }
            val declaredLength = connection.contentLengthLong
            require(declaredLength in 1..MAX_APK_BYTES) { "Update file size is invalid" }
            connection.inputStream.use { input ->
                temporaryApk.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_APK_BYTES) { "Update file is too large" }
                        output.write(buffer, 0, read)
                    }
                    require(total == declaredLength) { "Update download was incomplete" }
                }
            }
            require(sha256(temporaryApk).equals(update.sha256, ignoreCase = true)) { "Update checksum did not match" }
            require(temporaryApk.length() > 0L) { "Update file is empty" }
            requireArchiveMatchesInstalledApp(context, temporaryApk)
            if (apk.exists()) apk.delete()
            check(temporaryApk.renameTo(apk)) { "Could not prepare the update" }

            withContext(Dispatchers.Main) {
                val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            temporaryApk.delete()
            Log.w(TAG, "Update download or install handoff failed: ${e.javaClass.simpleName}")
            throw e
        } finally {
            connection.disconnect()
        }
    }

    private fun requireArchiveMatchesInstalledApp(context: Context, apk: File) {
        val packageManager = context.packageManager
        val archive = getPackageInfo(packageManager, apk.absolutePath, archive = true)
            ?: error("Downloaded file is not a valid Android app")
        require(archive.packageName == context.packageName) { "Update is for a different app" }

        val installed = getPackageInfo(packageManager, context.packageName, archive = false)
            ?: error("Could not verify the installed app")
        val archiveSigners = (archive.signingInfo?.apkContentsSigners ?: archive.signatures.orEmpty()).toSet()
        val installedSigners = (installed.signingInfo?.apkContentsSigners ?: installed.signatures.orEmpty()).toSet()
        require(archiveSigners.isNotEmpty() && archiveSigners == installedSigners) {
            "Update signature does not match this installation"
        }
        val archiveVersionCode = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        val installedVersionCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        require(archiveVersionCode > installedVersionCode) { "Update version is not newer" }
    }

    @Suppress("DEPRECATION")
    private fun getPackageInfo(packageManager: PackageManager, name: String, archive: Boolean): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return try {
            if (archive) packageManager.getPackageArchiveInfo(name, flags)
            else if (Build.VERSION.SDK_INT >= 33) packageManager.getPackageInfo(name, PackageManager.PackageInfoFlags.of(flags.toLong()))
            else packageManager.getPackageInfo(name, flags)
        } catch (_: Exception) {
            null
        }
    }

    private fun currentAppVersion(context: Context): Pair<Long, String> {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return versionCode to (info.versionName ?: "")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

internal fun parseUpdateManifest(
    manifestText: String,
    currentVersionCode: Long,
    currentVersion: String,
    expectedHost: String
): AppUpdateInfo? {
    val root = JSONObject(manifestText)
    val versionCode = root.optLong("version_code", 0L)
    val versionName = root.optString("version_name", "").trim()
    val downloadUrl = root.optString("download_url", "").trim()
    val checksum = root.optString("sha256", "").trim()
    if (versionCode <= currentVersionCode || versionName.isBlank() || !checksum.matches(Regex("[a-fA-F0-9]{64}"))) return null
    if (!downloadUrl.isTrustedUpdateUrl(expectedHost)) return null
    return AppUpdateInfo(
        latestVersion = versionName,
        versionCode = versionCode,
        currentVersion = currentVersion,
        downloadUrl = downloadUrl,
        sha256 = checksum,
        releaseNotes = root.optString("release_notes", "").take(4000)
    )
}

internal fun String.isTrustedUpdateUrl(expectedHost: String = "bymevpn.duckdns.org"): Boolean = runCatching {
    val uri = URI(this)
    uri.scheme.equals("https", ignoreCase = true) && uri.host.equals(expectedHost, ignoreCase = true) &&
        uri.userInfo == null && (uri.port == -1 || uri.port == 443)
}.getOrDefault(false)
