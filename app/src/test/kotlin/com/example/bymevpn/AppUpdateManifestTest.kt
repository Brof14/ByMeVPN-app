package com.example.bymevpn

import com.example.bymevpn.data.update.parseUpdateManifest
import com.example.bymevpn.data.update.copyUpdateAndHash
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppUpdateManifestTest {
    private val checksum = "a".repeat(64)

    @Test
    fun updateDownloadAcceptsUnknownLengthAndHashesTheBytesWritten() {
        val bytes = "signed update bytes".toByteArray()
        val output = ByteArrayOutputStream()
        val digest = copyUpdateAndHash(ByteArrayInputStream(bytes), output, -1, 1024)
        assertEquals(bytes.toList(), output.toByteArray().toList())
        assertEquals(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }, digest)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateDownloadRejectsTruncatedResponse() {
        copyUpdateAndHash(ByteArrayInputStream(byteArrayOf(1)), ByteArrayOutputStream(), 2, 1024)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateDownloadRejectsUnknownLengthAboveLimit() {
        copyUpdateAndHash(ByteArrayInputStream(byteArrayOf(1, 2)), ByteArrayOutputStream(), -1, 1)
    }

    @Test
    fun acceptsNewerVersionFromTrustedHttpsHost() {
        val update = parseUpdateManifest(
            """{"version_code":2,"version_name":"1.0.1","download_url":"https://bymevpn.duckdns.org/app/ByMeVPN.apk","sha256":"$checksum","release_notes":"Fixes"}""",
            currentVersionCode = 1,
            currentVersion = "1.0.0",
            expectedHost = "bymevpn.duckdns.org"
        )

        assertNotNull(update)
        assertEquals("1.0.1", update?.latestVersion)
        assertEquals(2L, update?.versionCode)
        assertEquals(checksum, update?.sha256)
    }

    @Test
    fun acceptsExactSignedReleaseAssetOnGitHub() {
        val update = parseUpdateManifest(
            """{"version_code":21,"version_name":"1.0.20","download_url":"https://github.com/Brof14/ByMeVPN-app/releases/download/v1.0.20/ByMeVPN-v1.0.20.apk","sha256":"$checksum","release_notes":"Fixes"}""",
            currentVersionCode = 20,
            currentVersion = "1.0.19",
            expectedHost = "github.com"
        )
        assertNotNull(update)
        assertEquals("1.0.20", update?.latestVersion)
    }

    @Test
    fun githubHostDoesNotTrustNonReleasePaths() {
        val update = parseUpdateManifest(
            manifest(url = "https://github.com/Brof14/ByMeVPN-app/releases/latest/download/ByMeVPN.apk"),
            currentVersionCode = 0,
            currentVersion = "1.0.0",
            expectedHost = "github.com"
        )
        assertNull(update)
    }

    @Test
    fun ignoresCurrentOrOlderVersion() {
        val current = manifest(versionCode = 1)
        val older = manifest(versionCode = 0)
        assertNull(parse(current, currentVersionCode = 1))
        assertNull(parse(older, currentVersionCode = 1))
    }

    @Test
    fun rejectsMissingChecksumAndUntrustedOrInsecureDownloadUrls() {
        assertNull(parse(manifest(checksum = "")))
        assertNull(parse(manifest(url = "http://bymevpn.duckdns.org/app/update.apk")))
        assertNull(parse(manifest(url = "https://github.com/Brof14/ByMeVPN-app/releases/latest")))
        assertNull(parse(manifest(url = "https://github.com/attacker/other/releases/download/v1.0.20/ByMeVPN-v1.0.20.apk")))
        assertNull(parse(manifest(url = "https://github.com/Brof14/ByMeVPN-app/releases/download/v1.0.20/other.apk")))
        assertNull(parse(manifest(url = "https://bymevpn.duckdns.org.attacker.example/update.apk")))
    }

    private fun parse(text: String, currentVersionCode: Long = 0) = parseUpdateManifest(
        text,
        currentVersionCode = currentVersionCode,
        currentVersion = "1.0.0",
        expectedHost = "bymevpn.duckdns.org"
    )

    private fun manifest(
        versionCode: Int = 1,
        checksum: String = this.checksum,
        url: String = "https://bymevpn.duckdns.org/app/ByMeVPN.apk"
    ) = """{"version_code":$versionCode,"version_name":"1.0.1","download_url":"$url","sha256":"$checksum"}"""
}
