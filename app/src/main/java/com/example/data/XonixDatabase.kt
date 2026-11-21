package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        UserAccountEntity::class,
        LoginHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class XonixDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var instance: XonixDatabase? = null

        fun getInstance(context: Context): XonixDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    XonixDatabase::class.java,
                    "xonix_studio_vault.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
