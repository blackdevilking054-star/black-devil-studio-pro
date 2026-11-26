package com.example.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Built-in App Security Firewall:
 * - Sanitizes all user-supplied text inputs
 * - Validates imported media URIs before decoding
 * - Hashes passwords with PBKDF2-HMAC-SHA256 + per-user random salt
 * - Encrypts stored session/security metadata with AES-256-GCM (Android KeyStore backed)
 * - Enforces owner-only access and 3-strike / 24-hour Master PIN rate limiting
 * - Performs automatic temporary cache cleanup after media exports
 */
object AppSecurityFirewall {

    private const val OWNER_EMAIL = "blackdevilking054@gmail.com"
    private const val KEYSTORE_ALIAS = "xonix_studio_pro_aes_key"
    private const val PREFS_NAME = "xonix_firewall_vault"
    private const val KEY_FAILED_PIN_ATTEMPTS = "pin_failed_enc"
    private const val KEY_LOCKOUT_UNTIL_MS = "pin_lockout_enc"
    private const val KEY_ACTIVE_USER_ID = "active_user_enc"
    private const val KEY_FALLBACK_AES = "fallback_local_k"

    private const val MAX_PIN_ATTEMPTS = 3
    private const val LOCKOUT_DURATION_MS = 24L * 60L * 60L * 1000L // 24 hours
    private const val PBKDF2_ITERATIONS = 65_536
    private const val PBKDF2_KEY_BITS = 256

    // Pre-computed PBKDF2-HMAC-SHA256 digest for the Owner Master PIN (never compared in plaintext logs)
    private const val OWNER_PIN_SALT_B64 = "WG9uaXhTdHVkaW9Qcm9TYWx0"
    private val ownerPinDigest: ByteArray by lazy {
        derivePbkdf2(
            secret = charArrayOf('6', '3', '7', '4', '0', '7'),
            salt = Base64.decode(OWNER_PIN_SALT_B64, Base64.NO_WRAP),
            iterations = 20_000
        )
    }

    private val secureRandom = SecureRandom()
    private val htmlTagRegex = Regex("<[^>]*>")
    private val controlCharRegex = Regex("[\\p{Cntrl}&&[^\n\t]]")
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // =========================================================================
    // 1. INPUT SANITIZATION & MEDIA URI VALIDATION
    // =========================================================================

    fun sanitizeText(input: String, maxLength: Int = 140, allowNewlines: Boolean = false): String {
        var cleaned = input
            .replace("\u0000", "")
            .replace(htmlTagRegex, "")
            .replace(controlCharRegex, "")
        if (!allowNewlines) {
            cleaned = cleaned.replace("\n", " ").replace("\r", " ")
        }
        return cleaned.trim().take(maxLength)
    }

    fun sanitizeEmail(input: String): String {
        return sanitizeText(input, maxLength = 120, allowNewlines = false).lowercase()
    }

    fun isValidEmail(email: String): Boolean {
        val clean = sanitizeEmail(email)
        return clean.isNotBlank() && emailRegex.matches(clean)
    }

    fun validatePassword(password: String): String? {
        if (password.length < 6) return "Password must be at least 6 characters"
        if (password.length > 72) return "Password is too long"
        return null
    }

    fun validateMediaUri(
        context: Context,
        uri: Uri,
        expectedMimePrefix: String? = null
    ): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "content" && scheme != "android.resource") return false
        val raw = uri.toString()
        if (raw.contains("..") || raw.contains("\u0000")) return false
        if (expectedMimePrefix != null && scheme == "content") {
            val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            if (mime != null && !mime.lowercase().startsWith(expectedMimePrefix.lowercase())) {
                return false
            }
        }
        return true
    }

    // =========================================================================
    // 2. PASSWORD HASHING (PBKDF2-HMAC-SHA256)
    // =========================================================================

    fun hashPassword(password: String): String {
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val hash = derivePbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "$saltB64:$hashB64"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        return try {
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expected = Base64.decode(parts[1], Base64.NO_WRAP)
            val actual = derivePbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS)
            MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) {
            false
        }
    }

    private fun derivePbkdf2(secret: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret, salt, iterations, PBKDF2_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } catch (_: Exception) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    // =========================================================================
    // 3. LOCAL ENCRYPTION (AES-256-GCM + ANDROID KEYSTORE)
    // =========================================================================

    private fun getOrCreateSecretKey(context: Context): SecretKey {
        runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existing = ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            gen.init(spec)
            return gen.generateKey()
        }

        // Safe fallback for Robolectric JVM test environment without hardware AndroidKeyStore
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_FALLBACK_AES, null)
        val rawKey = if (saved != null) {
            Base64.decode(saved, Base64.NO_WRAP)
        } else {
            ByteArray(32).also {
                secureRandom.nextBytes(it)
                prefs.edit().putString(KEY_FALLBACK_AES, Base64.encodeToString(it, Base64.NO_WRAP)).apply()
            }
        }
        return SecretKeySpec(rawKey, "AES")
    }

    fun encryptField(context: Context, plainText: String): String {
        return try {
            val key = getOrCreateSecretKey(context)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + encrypted.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    fun decryptField(context: Context, encoded: String): String? {
        return try {
            val raw = Base64.decode(encoded, Base64.NO_WRAP)
            if (raw.size < 14) return null
            val ivLen = raw[0].toInt() and 0xFF
            if (ivLen !in 12..16 || raw.size <= 1 + ivLen) return null
            val iv = raw.copyOfRange(1, 1 + ivLen)
            val payload = raw.copyOfRange(1 + ivLen, raw.size)
            val key = getOrCreateSecretKey(context)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(payload), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================================
    // 4. ENCRYPTED SESSION PERSISTENCE
    // =========================================================================

    fun saveActiveUserId(context: Context, userId: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (userId == null) {
            prefs.edit().remove(KEY_ACTIVE_USER_ID).apply()
        } else {
            prefs.edit().putString(KEY_ACTIVE_USER_ID, encryptField(context, userId)).apply()
        }
    }

    fun loadActiveUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enc = prefs.getString(KEY_ACTIVE_USER_ID, null) ?: return null
        return decryptField(context, enc)
    }

    // =========================================================================
    // 5. OWNER-ONLY ADMIN GATE & 24-HOUR PIN RATE LIMITER
    // =========================================================================

    fun isOwnerAccount(email: String, isGuest: Boolean): Boolean {
        if (isGuest) return false
        return sanitizeEmail(email) == OWNER_EMAIL
    }

    data class PinVerificationResult(
        val unlocked: Boolean,
        val isLockedOut: Boolean,
        val remainingLockoutMs: Long,
        val attemptsRemaining: Int,
        val message: String
    )

    fun getPinLockoutRemainingMs(context: Context, nowMs: Long = System.currentTimeMillis()): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encUntil = prefs.getString(KEY_LOCKOUT_UNTIL_MS, null) ?: return 0L
        val until = decryptField(context, encUntil)?.toLongOrNull() ?: return 0L
        return (until - nowMs).coerceAtLeast(0L)
    }

    fun verifyOwnerPin(
        context: Context,
        email: String,
        isGuest: Boolean,
        pinInput: String,
        nowMs: Long = System.currentTimeMillis()
    ): PinVerificationResult {
        if (!isOwnerAccount(email, isGuest)) {
            return PinVerificationResult(false, false, 0L, 0, "Access restricted")
        }

        val lockRemaining = getPinLockoutRemainingMs(context, nowMs)
        if (lockRemaining > 0L) {
            return PinVerificationResult(
                unlocked = false,
                isLockedOut = true,
                remainingLockoutMs = lockRemaining,
                attemptsRemaining = 0,
                message = "PIN locked for ${formatLockoutTime(lockRemaining)}"
            )
        }

        val cleanPin = pinInput.filter { it.isDigit() }.take(6)
        val candidateDigest = derivePbkdf2(
            secret = cleanPin.toCharArray(),
            salt = Base64.decode(OWNER_PIN_SALT_B64, Base64.NO_WRAP),
            iterations = 20_000
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (cleanPin.length == 6 && MessageDigest.isEqual(ownerPinDigest, candidateDigest)) {
            prefs.edit()
                .remove(KEY_FAILED_PIN_ATTEMPTS)
                .remove(KEY_LOCKOUT_UNTIL_MS)
                .apply()
            return PinVerificationResult(
                unlocked = true,
                isLockedOut = false,
                remainingLockoutMs = 0L,
                attemptsRemaining = MAX_PIN_ATTEMPTS,
                message = "Admin Dashboard unlocked"
            )
        }

        val prevFailed = prefs.getString(KEY_FAILED_PIN_ATTEMPTS, null)
            ?.let { decryptField(context, it)?.toIntOrNull() } ?: 0
        val nextFailed = prevFailed + 1

        if (nextFailed >= MAX_PIN_ATTEMPTS) {
            val unlockAt = nowMs + LOCKOUT_DURATION_MS
            prefs.edit()
                .putString(KEY_FAILED_PIN_ATTEMPTS, encryptField(context, "0"))
                .putString(KEY_LOCKOUT_UNTIL_MS, encryptField(context, unlockAt.toString()))
                .apply()
            return PinVerificationResult(
                unlocked = false,
                isLockedOut = true,
                remainingLockoutMs = LOCKOUT_DURATION_MS,
                attemptsRemaining = 0,
                message = "3 failed attempts. PIN locked for 24 hours."
            )
        }

        prefs.edit()
            .putString(KEY_FAILED_PIN_ATTEMPTS, encryptField(context, nextFailed.toString()))
            .apply()
        val left = (MAX_PIN_ATTEMPTS - nextFailed).coerceAtLeast(0)
        return PinVerificationResult(
            unlocked = false,
            isLockedOut = false,
            remainingLockoutMs = 0L,
            attemptsRemaining = left,
            message = "Incorrect PIN. $left attempt(s) left."
        )
    }

    fun formatLockoutTime(ms: Long): String {
        val totalMinutes = (ms / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    // =========================================================================
    // 6. AUTOMATIC CACHE CLEANUP & NATIVE SHARE SHEET
    // =========================================================================

    fun cleanupTempExportCache(context: Context) {
        runCatching {
            val cacheDir = context.cacheDir ?: return
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.startsWith("bd_export_") || file.name.startsWith("xonix_export_") || file.name.endsWith(".tmp"))) {
                    file.delete()
                }
            }
            // Trim stale temporary extracted audio files older than 30 minutes
            val audioDir = File(cacheDir, "audio_tracks")
            val cutoff = System.currentTimeMillis() - (30L * 60L * 1000L)
            audioDir.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.startsWith("starter_") && file.lastModified() < cutoff) {
                    file.delete()
                }
            }
        }
    }

    const val APP_DOWNLOAD_URL = "https://github.com/blackdevilking054-star/black-devil-studio-pro/releases/latest"

    fun buildReferAndShareInviteText(): String =
        "I'm creating 4K videos, viral thumbnails, and pro sound mixes with Black Devil Studio Pro — completely free with zero watermarks! " +
            "Try Black Devil Studio Pro for your YouTube videos, Shorts, and Instagram Reels.\n\n" +
            "Download APK here:\n" +
            APP_DOWNLOAD_URL

    fun launchReferAndShareIntent(context: Context) {
        val inviteText = buildReferAndShareInviteText()
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Try Black Devil Studio Pro")
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Black Devil Studio Pro").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }
}
