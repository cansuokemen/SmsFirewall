package com.example.smsfirewall.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_messages",
    indices = [
        Index("status"),
        Index(value = ["status", "receivedAt"])
    ]
)
data class SmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val status: String,
    val reason: String,
    @ColumnInfo(name = "is_starred", defaultValue = "0")
    val isStarred: Boolean = false,
)
