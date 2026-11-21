package com.example.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.security.AppSecurityFirewall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersionName: String, val currentVersionCode: Int) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

/**
 * GitHub Chrome OTA In-App Update Checker.
 * Fetches release metadata from GitHub raw JSON and opens verified HTTPS APK links in Chrome.
 */
object AppUpdateChecker {

    const val VERSION_JSON_URL =
        "https://raw.githubusercontent.com/blackdevilking054-star/black-devil-studio-pro/main/version.json"

    private const val CHROME_PACKAGE = "com.android.chrome"
    private const val CONNECT_TIMEOUT_MS = 7_000
    private const val READ_TIMEOUT_MS = 7_000
    private const val MAX_RESPONSE_BYTES = 32 * 1024 // 32 KB cap for safety

    private val allowedHostExact = setOf(
        "github.com",
        "raw.githubusercontent.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "codeload.github.com"
    )

    private val allowedHostSuffixes = listOf(
        ".github.com",
        ".githubusercontent.com",
        ".github.io"
    )

    suspend fun checkForUpdate(
        currentVersionCode: Int,
        currentVersionName: String,
        metadataUrl: String = VERSION_JSON_URL
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isTrustedGitHubHttpsUrl(metadataUrl)) {
            return@withContext UpdateCheckResult.Failed("Update server URL is not trusted.")
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(metadataUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Accept", "application/json")
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext UpdateCheckResult.Failed("Could not reach update server (HTTP $code).")
            }

            val finalHost = connection.url?.toString().orEmpty()
            if (finalHost.isNotBlank() && !isTrustedGitHubHttpsUrl(finalHost)) {
                return@withContext UpdateCheckResult.Failed("Blocked untrusted redirect while checking updates.")
            }

            val body = connection.inputStream.use { stream ->
                readBoundedUtf8(stream, MAX_RESPONSE_BYTES)
            }
            parseVersionJson(body, currentVersionCode, currentVersionName)
        } catch (_: Exception) {
            UpdateCheckResult.Failed("Unable to check for updates. Check your internet connection.")
        } finally {
            connection?.disconnect()
        }
    }

    internal fun parseVersionJson(
        rawJson: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): UpdateCheckResult {
        if (rawJson.isBlank()) {
            return UpdateCheckResult.Failed("Update metadata is empty.")
        }
        return try {
            val json = JSONObject(rawJson)
            val remoteVersionCode = json.optInt("versionCode", -1)
            if (remoteVersionCode <= 0) {
                return UpdateCheckResult.Failed("Invalid versionCode in update metadata.")
            }

            if (remoteVersionCode <= currentVersionCode) {
                return UpdateCheckResult.UpToDate(
                    currentVersionName = currentVersionName,
                    currentVersionCode = currentVersionCode
                )
            }

            val rawVersionName = json.optString("versionName", "")
                .ifBlank { "v$remoteVersionCode" }
            val safeVersionName = AppSecurityFirewall.sanitizeText(
                input = rawVersionName,
                maxLength = 40,
                allowNewlines = false
            ).ifBlank { "$remoteVersionCode" }

            val rawApkUrl = json.optString("apkUrl", "")
                .ifBlank { json.optString("downloadUrl", "") }
                .ifBlank { json.optString("url", "") }
                .trim()

            if (!isTrustedGitHubHttpsUrl(rawApkUrl)) {
                return UpdateCheckResult.Failed("Update download link is missing or not a trusted HTTPS GitHub URL.")
            }

            val rawNotes = json.optString("releaseNotes", "")
                .ifBlank { json.optString("changelog", "") }
                .ifBlank { json.optString("notes", "") }
                .ifBlank { json.optString("message", "") }
            val safeNotes = AppSecurityFirewall.sanitizeText(
                input = rawNotes,
                maxLength = 400,
                allowNewlines = true
            )

            UpdateCheckResult.UpdateAvailable(
                AppUpdateInfo(
                    versionCode = remoteVersionCode,
                    versionName = safeVersionName,
                    apkUrl = rawApkUrl,
                    releaseNotes = safeNotes
                )
            )
        } catch (_: Exception) {
            UpdateCheckResult.Failed("Could not read update details from server.")
        }
    }

    fun isTrustedGitHubHttpsUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty() || trimmed.length > 1024) return false
        if (trimmed.any { it.isWhitespace() || it.code < 0x20 }) return false

        val parsed = runCatching { URI(trimmed) }.getOrNull() ?: return false
        if (!parsed.scheme.equals("https", ignoreCase = true)) return false
        if (!parsed.userInfo.isNullOrEmpty()) return false

        val host = parsed.host?.lowercase()?.trimEnd('.') ?: return false
        if (host.isBlank()) return false

        return host in allowedHostExact || allowedHostSuffixes.any { suffix ->
            host.endsWith(suffix) && host.length > suffix.length
        }
    }

    /**
     * Opens the validated APK URL directly in Google Chrome, falling back to the
     * system default browser if Chrome is not installed on the device.
     */
    fun openApkInChrome(context: Context, apkUrl: String): Boolean {
        val trimmed = apkUrl.trim()
        if (!isTrustedGitHubHttpsUrl(trimmed)) return false

        val uri = Uri.parse(trimmed)
        val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(CHROME_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val launchedInChrome = runCatching {
            if (chromeIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chromeIntent)
                true
            } else {
                false
            }
        }.getOrDefault(false)

        if (launchedInChrome) return true

        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(fallbackIntent)
            true
        }.getOrDefault(false)
    }

    private fun readBoundedUtf8(input: InputStream, maxBytes: Int): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            val allowed = minOf(read, maxBytes - total)
            if (allowed > 0) {
                out.write(buffer, 0, allowed)
                total += allowed
            }
            if (total >= maxBytes) break
        }
        return out.toString(Charsets.UTF_8.name())
    }
}
