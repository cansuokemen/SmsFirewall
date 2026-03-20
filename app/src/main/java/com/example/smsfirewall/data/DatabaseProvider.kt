package com.example.smsfirewall.data

import android.content.Context
import androidx.room.Room
import com.example.smsfirewall.data.local.AppDatabase

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "sms_firewall.db"
            )
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build().also { db ->
                instance = db
            }
        }
    }
}
