package com.example.data

import android.content.Context
import com.example.security.AppSecurityFirewall
import kotlinx.coroutines.flow.Flow
import java.util.UUID

sealed class AuthResult {
    data class Success(val user: UserAccountEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Clean Firebase-ready User & Auth Repository interface.
 * Local Room SQLite implementation works 100% offline, and a Firebase Firestore/Auth sync
 * implementation can be plugged in behind this exact contract without changing ViewModel or UI.
 */
interface UserAuthRepository {
    val allUsers: Flow<List<UserAccountEntity>>
    val allLoginHistory: Flow<List<LoginHistoryEntity>>

    fun observeUserLoginHistory(userId: String): Flow<List<LoginHistoryEntity>>
    suspend fun restoreSession(): UserAccountEntity?
    suspend fun continueAsGuest(): AuthResult
    suspend fun createAccount(fullName: String, email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun updateProfileName(userId: String, newName: String): AuthResult
    suspend fun removeAccount(userId: String)
    suspend fun logOut()
}

class LocalEncryptedUserRepository(
    private val context: Context,
    private val userDao: UserDao
) : UserAuthRepository {

    override val allUsers: Flow<List<UserAccountEntity>> = userDao.observeAllUsers()
    override val allLoginHistory: Flow<List<LoginHistoryEntity>> = userDao.observeAllLoginHistory()

    override fun observeUserLoginHistory(userId: String): Flow<List<LoginHistoryEntity>> {
        return userDao.observeLoginHistoryForUser(userId)
    }

    override suspend fun restoreSession(): UserAccountEntity? {
        val activeId = AppSecurityFirewall.loadActiveUserId(context) ?: return null
        return userDao.getUserById(activeId)
    }

    override suspend fun continueAsGuest(): AuthResult {
        val now = System.currentTimeMillis()
        val guestEmail = "guest_${UUID.randomUUID().toString().take(6)}@local.xonix"
        val guestId = "guest_${UUID.randomUUID().toString().take(8)}"
        val guest = UserAccountEntity(
            id = guestId,
            fullName = "Guest Creator",
            email = guestEmail,
            passwordHash = "",
            isGuest = true,
            createdAt = now,
            lastLoginAt = now,
            encryptedProfileMeta = AppSecurityFirewall.encryptField(context, "offline_guest")
        )
        userDao.upsertUser(guest)
        userDao.insertLoginHistory(
            LoginHistoryEntity(
                userId = guest.id,
                userName = guest.fullName,
                userEmail = "Guest Mode",
                isGuest = true,
                loginTimestamp = now,
                eventType = "Guest Session"
            )
        )
        AppSecurityFirewall.saveActiveUserId(context, guest.id)
        return AuthResult.Success(guest)
    }

    override suspend fun createAccount(fullName: String, email: String, password: String): AuthResult {
        val cleanName = AppSecurityFirewall.sanitizeText(fullName, maxLength = 60)
        val cleanEmail = AppSecurityFirewall.sanitizeEmail(email)
        if (cleanName.length < 2) {
            return AuthResult.Error("Please enter your full name")
        }
        if (!AppSecurityFirewall.isValidEmail(cleanEmail)) {
            return AuthResult.Error("Please enter a valid email address")
        }
        val pwError = AppSecurityFirewall.validatePassword(password)
        if (pwError != null) {
            return AuthResult.Error(pwError)
        }
        if (userDao.getUserByEmail(cleanEmail) != null) {
            return AuthResult.Error("An account with this email already exists. Please sign in.")
        }

        val now = System.currentTimeMillis()
        val user = UserAccountEntity(
            id = "usr_${UUID.randomUUID().toString().take(10)}",
            fullName = cleanName,
            email = cleanEmail,
            passwordHash = AppSecurityFirewall.hashPassword(password),
            isGuest = false,
            createdAt = now,
            lastLoginAt = now,
            encryptedProfileMeta = AppSecurityFirewall.encryptField(context, "registered_local_v1")
        )
        userDao.upsertUser(user)
        userDao.insertLoginHistory(
            LoginHistoryEntity(
                userId = user.id,
                userName = user.fullName,
                userEmail = user.email,
                isGuest = false,
                loginTimestamp = now,
                eventType = "Account Created"
            )
        )
        AppSecurityFirewall.saveActiveUserId(context, user.id)
        return AuthResult.Success(user)
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        val cleanEmail = AppSecurityFirewall.sanitizeEmail(email)
        if (!AppSecurityFirewall.isValidEmail(cleanEmail)) {
            return AuthResult.Error("Please enter a valid email address")
        }
        if (password.isBlank()) {
            return AuthResult.Error("Please enter your password")
        }

        val existing = userDao.getUserByEmail(cleanEmail)
            ?: return AuthResult.Error("No account found for that email")
        if (existing.isGuest || !AppSecurityFirewall.verifyPassword(password, existing.passwordHash)) {
            return AuthResult.Error("Incorrect email or password")
        }

        val now = System.currentTimeMillis()
        userDao.updateLastLogin(existing.id, now)
        val updated = existing.copy(lastLoginAt = now)
        userDao.insertLoginHistory(
            LoginHistoryEntity(
                userId = updated.id,
                userName = updated.fullName,
                userEmail = updated.email,
                isGuest = false,
                loginTimestamp = now,
                eventType = "Sign In"
            )
        )
        AppSecurityFirewall.saveActiveUserId(context, updated.id)
        return AuthResult.Success(updated)
    }

    override suspend fun updateProfileName(userId: String, newName: String): AuthResult {
        val clean = AppSecurityFirewall.sanitizeText(newName, maxLength = 60)
        if (clean.length < 2) {
            return AuthResult.Error("Name must be at least 2 characters")
        }
        userDao.updateFullName(userId, clean)
        val updated = userDao.getUserById(userId)
            ?: return AuthResult.Error("Account not found")
        return AuthResult.Success(updated)
    }

    override suspend fun removeAccount(userId: String) {
        userDao.deleteLoginHistoryForUser(userId)
        userDao.deleteUserById(userId)
        if (AppSecurityFirewall.loadActiveUserId(context) == userId) {
            AppSecurityFirewall.saveActiveUserId(context, null)
        }
    }

    override suspend fun logOut() {
        AppSecurityFirewall.saveActiveUserId(context, null)
    }
}
