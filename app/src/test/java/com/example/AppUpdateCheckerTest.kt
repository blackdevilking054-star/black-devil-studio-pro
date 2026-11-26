package com.example

import com.example.updater.AppUpdateChecker
import com.example.updater.UpdateCheckResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppUpdateCheckerTest {

    @Test
    fun parseVersionJson_newerVersionWithValidGitHubApk_returnsUpdateAvailable() {
        val json = """
            {
              "versionCode": 3,
              "versionName": "2.1",
              "apkUrl": "https://github.com/blackdevilking054-star/black-devil-studio-pro/releases/download/v2.1/app-release.apk",
              "releaseNotes": "Faster 4K export and <b>new FX</b>"
            }
        """.trimIndent()

        val result = AppUpdateChecker.parseVersionJson(
            rawJson = json,
            currentVersionCode = 2,
            currentVersionName = "2.0"
        )

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val info = (result as UpdateCheckResult.UpdateAvailable).info
        assertEquals(3, info.versionCode)
        assertEquals("2.1", info.versionName)
        assertEquals(
            "https://github.com/blackdevilking054-star/black-devil-studio-pro/releases/download/v2.1/app-release.apk",
            info.apkUrl
        )
        assertEquals("Faster 4K export and new FX", info.releaseNotes)
    }

    @Test
    fun parseVersionJson_sameOrOlderVersion_returnsUpToDate() {
        val json = """
            {
              "versionCode": 2,
              "versionName": "2.0",
              "apkUrl": "https://github.com/blackdevilking054-star/black-devil-studio-pro/releases/download/v2.0/app-release.apk"
            }
        """.trimIndent()

        val result = AppUpdateChecker.parseVersionJson(
            rawJson = json,
            currentVersionCode = 2,
            currentVersionName = "2.0"
        )

        assertTrue(result is UpdateCheckResult.UpToDate)
    }

    @Test
    fun parseVersionJson_untrustedOrNonHttpsApkUrl_isRejected() {
        val httpJson = """
            {
              "versionCode": 5,
              "versionName": "3.0",
              "apkUrl": "http://github.com/blackdevilking054-star/black-devil-studio-pro/releases/latest/app.apk"
            }
        """.trimIndent()
        val evilJson = """
            {
              "versionCode": 5,
              "versionName": "3.0",
              "apkUrl": "https://evil.example.com/fake.apk"
            }
        """.trimIndent()

        assertTrue(AppUpdateChecker.parseVersionJson(httpJson, 2, "2.0") is UpdateCheckResult.Failed)
        assertTrue(AppUpdateChecker.parseVersionJson(evilJson, 2, "2.0") is UpdateCheckResult.Failed)
    }

    @Test
    fun isTrustedGitHubHttpsUrl_validatesHostsAndSchemes() {
        assertTrue(AppUpdateChecker.isTrustedGitHubHttpsUrl(AppUpdateChecker.VERSION_JSON_URL))
        assertTrue(
            AppUpdateChecker.isTrustedGitHubHttpsUrl(
                "https://objects.githubusercontent.com/github-production-release-asset/app.apk"
            )
        )
        assertFalse(AppUpdateChecker.isTrustedGitHubHttpsUrl("javascript:alert(1)"))
        assertFalse(AppUpdateChecker.isTrustedGitHubHttpsUrl("file:///sdcard/app.apk"))
        assertFalse(AppUpdateChecker.isTrustedGitHubHttpsUrl("https://user@github.com/repo/app.apk"))
        assertFalse(AppUpdateChecker.isTrustedGitHubHttpsUrl("https://notgithub.com/repo/app.apk"))
    }

    @Test
    fun shouldShowUpdateDialog_respectsVersionCodeAndDismissedVersion() {
        // Installed v2.5 (5) vs remote v2.5 (5): never shows (auto or manual)
        assertFalse(AppUpdateChecker.shouldShowUpdateDialog(remoteVersionCode = 5, currentVersionCode = 5, dismissedVersionCode = 0, manualCheck = false))
        assertFalse(AppUpdateChecker.shouldShowUpdateDialog(remoteVersionCode = 5, currentVersionCode = 5, dismissedVersionCode = 0, manualCheck = true))

        // Remote v2.6 (6) when not yet dismissed: shows on auto and manual
        assertTrue(AppUpdateChecker.shouldShowUpdateDialog(remoteVersionCode = 6, currentVersionCode = 5, dismissedVersionCode = 0, manualCheck = false))
        assertTrue(AppUpdateChecker.shouldShowUpdateDialog(remoteVersionCode = 6, currentVersionCode = 5, dismissedVersionCode = 0, manualCheck = true))

        // After tapping "Later" on version 6: auto check stays silent, manual check still shows
        assertFalse(AppUpdateChecker.shouldShowUpdateDialog(remoteVersionCode = 6, currentVersionCode = 5, dismissedVersionCode = 6, manualCheck = false))
        assertTrue(AppUpdateChecker.shouldShowUpdateDialog(remoteVersionCode = 6, currentVersionCode = 5, dismissedVersionCode = 6, manualCheck = true))

        // When version 7 is published after dismissing 6: auto check shows again
        assertTrue(AppUpdateChecker.shouldShowUpdateDialog(remoteVersionCode = 7, currentVersionCode = 5, dismissedVersionCode = 6, manualCheck = false))
    }
}

