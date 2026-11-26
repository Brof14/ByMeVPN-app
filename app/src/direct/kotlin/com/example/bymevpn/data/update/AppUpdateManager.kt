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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
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
    // GitHub Releases is the canonical update source. This avoids making app
    // updates depend on the VPS upload host or its SSH deployment key.
    private const val MANIFEST_URL = "https://github.com/Brof14/ByMeVPN-app/releases/latest/download/update.json"
    private const val MAX_APK_BYTES = 200L * 1024L * 1024L
    private const val PREFS_NAME = "app_update_cache"
    private const val KEY_LAST_ATTEMPT = "last_attempt_at"
    private const val KEY_LAST_SUCCESS = "last_success_at"
    private const val KEY_CACHED_MANIFEST = "manifest"
    private const val UPDATE_CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000
    private const val FAILED_CHECK_RETRY_MS = 10L * 60 * 1000

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    private val updateCheckMutex = Mutex()
    val updateInfo = _updateInfo.asStateFlow()

    suspend fun checkForUpdates(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            _updateInfo.value = null
            return@withContext null
        }
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val current = currentAppVersion(appContext)
        val lastSuccess = preferences.getLong(KEY_LAST_SUCCESS, 0L)
        val lastAttempt = preferences.getLong(KEY_LAST_ATTEMPT, 0L)
        val freshSuccess = lastSuccess > 0L && now >= lastSuccess && now - lastSuccess < UPDATE_CHECK_INTERVAL_MS
        val recentFailure = lastAttempt > 0L && now >= lastAttempt && now - lastAttempt < FAILED_CHECK_RETRY_MS
        if (freshSuccess || recentFailure) {
            val cachedManifest = preferences.getString(KEY_CACHED_MANIFEST, null)
            val cachedUpdate = cachedManifest?.let {
                parseUpdateManifest(it, current.first, current.second, "github.com", Build.SUPPORTED_ABIS.toList())
            }
            _updateInfo.value = cachedUpdate
            return@withContext cachedUpdate
        }
        // Activity resumes can overlap; a slower failed request must not erase a newer result.
        if (!updateCheckMutex.tryLock()) return@withContext _updateInfo.value
        var connection: HttpURLConnection? = null
        try {
            preferences.edit().putLong(KEY_LAST_ATTEMPT, now).apply()
            val activeConnection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ByMeVPN-Android")
            }
            connection = activeConnection
            if (activeConnection.responseCode !in 200..299) {
                return@withContext _updateInfo.value
            }
            require(activeConnection.contentLengthLong in -1..32_768L) { "Update manifest is too large" }
            val manifestText = activeConnection.inputStream.use { input ->
                val bytes = input.readNBytesCompat(32_769)
                require(bytes.size <= 32_768) { "Update manifest is too large" }
                bytes.toString(Charsets.UTF_8)
            }
            preferences.edit()
                .putString(KEY_CACHED_MANIFEST, manifestText)
                .putLong(KEY_LAST_SUCCESS, System.currentTimeMillis())
                .commit()
            val update = parseUpdateManifest(manifestText, current.first, current.second, "github.com", Build.SUPPORTED_ABIS.toList())
            _updateInfo.value = update
            update
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Network and parsing errors are expected on offline launches; never log URLs or response bodies.
            Log.w(TAG, "Update check failed: ${e.javaClass.simpleName}")
            _updateInfo.value
        } finally {
            connection?.disconnect()
            updateCheckMutex.unlock()
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
            // GitHub release assets redirect to its signed asset CDN.
            instanceFollowRedirects = true
            connectTimeout = 15000
            readTimeout = 30000
            setRequestProperty("User-Agent", "ByMeVPN-Android")
        }

        try {
            require(connection.responseCode in 200..299) { "Could not download the update" }
            val declaredLength = connection.contentLengthLong
            require(declaredLength == -1L || declaredLength in 1..MAX_APK_BYTES) { "Update file size is invalid" }
            val actualHash = connection.inputStream.use { input ->
                temporaryApk.outputStream().buffered().use { output ->
                    copyUpdateAndHash(input, output, declaredLength, MAX_APK_BYTES)
                }
            }
            require(actualHash.equals(update.sha256, ignoreCase = true)) { "Update checksum did not match" }
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
        val archiveSigners = extractSigners(archive)
        val installedSigners = extractSigners(installed)
        require(archiveSigners.isNotEmpty() && archiveSigners == installedSigners) {
            "Update signature does not match this installation"
        }
        val archiveVersionCode = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        val installedVersionCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        require(archiveVersionCode > installedVersionCode) { "Update version is not newer" }
    }

    @Suppress("DEPRECATION")
    private fun extractSigners(info: PackageInfo): Set<android.content.pm.Signature> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners.orEmpty().toSet()
        } else {
            info.signatures.orEmpty().toSet()
        }
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

}

internal fun copyUpdateAndHash(input: InputStream, output: OutputStream, declaredLength: Long, limit: Long): String {
    require(declaredLength == -1L || declaredLength in 1..limit) { "Update file size is invalid" }
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= limit) { "Update file is too large" }
        output.write(buffer, 0, read)
        digest.update(buffer, 0, read)
    }
    require(total > 0 && (declaredLength == -1L || total == declaredLength)) { "Update download was incomplete" }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (output.size() < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - output.size()))
        if (read < 0) break
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

internal fun parseUpdateManifest(
    manifestText: String,
    currentVersionCode: Long,
    currentVersion: String,
    expectedHost: String,
    supportedAbis: List<String> = emptyList()
): AppUpdateInfo? {
    val root = JSONObject(manifestText)
    val versionCode = root.optLong("version_code", 0L)
    val versionName = root.optString("version_name", "").trim()
    val abiDownloads = root.optJSONObject("abi_downloads")
    val selectedDownload = supportedAbis.firstNotNullOfOrNull { abiDownloads?.optJSONObject(it) }
        ?: abiDownloads?.optJSONObject("universal")
        ?: root
    val downloadUrl = selectedDownload.optString("download_url", "").trim()
    val checksum = selectedDownload.optString("sha256", "").trim()
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
    val secureOrigin = uri.scheme.equals("https", ignoreCase = true) &&
        uri.userInfo == null && (uri.port == -1 || uri.port == 443)
    val legacyBymeOrigin = expectedHost.equals("bymevpn.duckdns.org", ignoreCase = true) &&
        uri.host.equals(expectedHost, ignoreCase = true)
    val githubRelease = uri.host.equals("github.com", ignoreCase = true) &&
        uri.path.matches(Regex("/Brof14/ByMeVPN-app/releases/download/v\\d+\\.\\d+\\.\\d+/ByMeVPN-v\\d+\\.\\d+\\.\\d+(-(arm64-v8a|armeabi-v7a|x86|x86_64))?\\.apk"))
    secureOrigin && (legacyBymeOrigin || githubRelease)
}.getOrDefault(false)
