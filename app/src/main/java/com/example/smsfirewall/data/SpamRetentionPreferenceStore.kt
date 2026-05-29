package com.example.smsfirewall.data

import android.content.Context
import android.content.SharedPreferences

class SpamRetentionPreferenceStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRetentionDays(): Int {
        return prefs.getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)
    }

    fun setRetentionDays(days: Int) {
        require(days in RETENTION_OPTIONS) { "Geçersiz saklama süresi: $days" }
        prefs.edit().putInt(KEY_RETENTION_DAYS, days).apply()
    }

    fun getRetentionMillis(): Long {
        return getRetentionDays().toLong() * 24L * 60L * 60L * 1000L
    }

    fun cutoffTimestamp(now: Long = System.currentTimeMillis()): Long {
        return now - getRetentionMillis()
    }

    companion object {
        private const val PREFS_NAME = "spam_retention_prefs"
        private const val KEY_RETENTION_DAYS = "retention_days"
        const val DEFAULT_RETENTION_DAYS = 30
        val RETENTION_OPTIONS = listOf(30, 60, 90)
    }
}
