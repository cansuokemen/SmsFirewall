package com.example.smsfirewall.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SmsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao
}
