package com.example.smsfirewall.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SmsEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_messages_status` ON `sms_messages` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_messages_status_receivedAt` ON `sms_messages` (`status`, `receivedAt`)")
            }
        }
    }
}
