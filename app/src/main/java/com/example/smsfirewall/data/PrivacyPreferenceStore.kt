package com.example.smsfirewall.data

import android.content.Context
import com.example.smsfirewall.data.security.CryptoBox

class PrivacyPreferenceStore(
    context: Context,
    private val cryptoBox: CryptoBox
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getTrashRetentionDays(): Int =
        readProtectedString(KEY_TRASH_RETENTION_DAYS, null)?.toIntOrNull()
            ?: readLegacyInt(KEY_TRASH_RETENTION_DAYS, DEFAULT_TRASH_RETENTION_DAYS)

    fun setTrashRetentionDays(days: Int) {
        writeProtectedString(KEY_TRASH_RETENTION_DAYS, days.toString())
    }

    fun isPrivateAreaEncryptionEnabled(): Boolean =
        readProtectedString(KEY_PRIVATE_AREA_ENCRYPTION, null)?.toBooleanStrictOrNull()
            ?: readLegacyBoolean(KEY_PRIVATE_AREA_ENCRYPTION, false)

    fun setPrivateAreaEncryptionEnabled(enabled: Boolean) {
        writeProtectedString(KEY_PRIVATE_AREA_ENCRYPTION, enabled.toString())
    }

    fun trashCutoffTimestamp(now: Long = System.currentTimeMillis()): Long {
        val days = getTrashRetentionDays()
        if (days == RETENTION_NEVER) return Long.MIN_VALUE
        return now - days * 24L * 60L * 60L * 1000L
    }

    private fun readProtectedString(key: String, fallback: String?): String? {
        val raw = prefs.all[key] as? String ?: return fallback
        return cryptoBox.decrypt(raw).ifBlank { fallback }
    }

    private fun writeProtectedString(key: String, value: String) {
        prefs.edit().putString(key, cryptoBox.encrypt(value)).apply()
    }

    private fun readLegacyInt(key: String, fallback: Int): Int {
        val raw = prefs.all[key]
        val value = raw as? Int ?: return fallback
        writeProtectedString(key, value.toString())
        return value
    }

    private fun readLegacyBoolean(key: String, fallback: Boolean): Boolean {
        val raw = prefs.all[key]
        val value = raw as? Boolean ?: return fallback
        writeProtectedString(key, value.toString())
        return value
    }

    companion object {
        const val RETENTION_NEVER = -1
        const val DEFAULT_TRASH_RETENTION_DAYS = 30
        val TRASH_RETENTION_OPTIONS = listOf(7, 30, 90, RETENTION_NEVER)

        private const val PREF_NAME = "privacy_preferences"
        private const val KEY_TRASH_RETENTION_DAYS = "trash_retention_days"
        private const val KEY_PRIVATE_AREA_ENCRYPTION = "private_area_encryption"
    }
}
