package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_accounts ORDER BY lastLoginAt DESC")
    fun observeAllUsers(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserAccountEntity)

    @Query("UPDATE user_accounts SET fullName = :fullName WHERE id = :userId")
    suspend fun updateFullName(userId: String, fullName: String)

    @Query("UPDATE user_accounts SET lastLoginAt = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long)

    @Query("DELETE FROM user_accounts WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("SELECT * FROM login_history ORDER BY loginTimestamp DESC LIMIT 200")
    fun observeAllLoginHistory(): Flow<List<LoginHistoryEntity>>

    @Query("SELECT * FROM login_history WHERE userId = :userId ORDER BY loginTimestamp DESC LIMIT 50")
    fun observeLoginHistoryForUser(userId: String): Flow<List<LoginHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoginHistory(entry: LoginHistoryEntity)

    @Query("DELETE FROM login_history WHERE userId = :userId")
    suspend fun deleteLoginHistoryForUser(userId: String)
}
