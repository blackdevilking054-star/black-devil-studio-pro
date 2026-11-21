package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val isGuest: Boolean,
    val createdAt: Long,
    val lastLoginAt: Long,
    val encryptedProfileMeta: String = ""
)

@Entity(tableName = "login_history")
data class LoginHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val isGuest: Boolean,
    val loginTimestamp: Long,
    val eventType: String
)
