package com.example.smsfirewall.data

object SpamRetentionPolicy {
    const val RETENTION_MILLIS: Long = 24L * 60L * 60L * 1000L

    fun cutoffTimestamp(now: Long = System.currentTimeMillis()): Long {
        return now - RETENTION_MILLIS
    }
}
