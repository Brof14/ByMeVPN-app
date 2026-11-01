package com.example.bymevpn

import com.example.bymevpn.data.update.parseUpdateManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppUpdateManifestTest {
    private val checksum = "a".repeat(64)

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
