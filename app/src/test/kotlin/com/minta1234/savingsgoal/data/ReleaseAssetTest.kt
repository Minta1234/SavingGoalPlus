package com.minta1234.savingsgoal.data

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ReleaseAssetTest {
    private val url = "https://github.com/Minta1234/SavingGoalPlus/releases/download/v1.2.0/app-release.apk"

    @Test fun decodesReleaseAndSelectsOnlyApk() {
        val release = Json { ignoreUnknownKeys = true }.decodeFromString<ReleaseNote>("""
            {"tag_name":"v1.2.0","html_url":"https://github.com/Minta1234/SavingGoalPlus/releases/tag/v1.2.0",
             "assets":[{"name":"app-release.apk","browser_download_url":"$url","size":123,"digest":"sha256:abc","id":1},
             {"name":"ios.ipa","browser_download_url":"https://github.com/Minta1234/SavingGoalPlus/releases/download/v1.2.0/ios.ipa","size":123}]}
        """.trimIndent())
        assertEquals("app-release.apk", release.androidApk?.name)
        assertEquals("sha256:abc", release.androidApk?.digest)
        assertNull(release.copy(assets = release.assets + release.assets.first()).androidApk)
        assertNull(release.copy(assets = emptyList()).androidApk)
    }

    @Test fun acceptsOlderReleasesWithoutAssetsOrDigest() {
        val release = Json.decodeFromString<ReleaseNote>("""{"tag_name":"v1.0.0","html_url":"https://github.com/Minta1234/SavingGoalPlus/releases/tag/v1.0.0"}""")
        assertTrue(release.assets.isEmpty())
        assertNull(ReleaseAsset("app.apk", url, 123).digest)
    }

    @Test fun rejectsOtherRepositoriesAndInvalidDownloads() {
        val invalid = listOf(
            url.replace("https:", "http:"), url.replace("github.com", "github.com.evil.test"),
            url.replace("Minta1234", "another-owner"), url.replace("SavingGoalPlus", "other-app"),
            url.replace("github.com", "user@github.com"), url.replace("github.com", "github.com:8443"),
            "not a URL", "file:///tmp/update.apk"
        )
        invalid.forEach { assertFalse(it, ReleaseAsset("app.apk", it, 123).isApk) }
        assertFalse(ReleaseAsset("app.apk", url, 0).isApk)
        assertFalse(ReleaseAsset("app.apk", url, ReleaseAsset.MAX_APK_BYTES + 1).isApk)
        assertFalse(ReleaseAsset("app.zip", url, 123).isApk)
    }

    @Test fun downloadRedirectsRequireTrustedHttpsHosts() {
        assertTrue(isUpdateDownloadUrl(url))
        assertTrue(isUpdateDownloadUrl("https://release-assets.githubusercontent.com/github-production-release-asset/123?token=example"))
        assertFalse(isUpdateDownloadUrl("http://release-assets.githubusercontent.com/file"))
        assertFalse(isUpdateDownloadUrl("https://release-assets.githubusercontent.com.evil.test/file"))
        assertFalse(isUpdateDownloadUrl("https://example.com/file.apk"))
        assertFalse(isUpdateDownloadUrl("https://user@github.com/file"))
    }
}
