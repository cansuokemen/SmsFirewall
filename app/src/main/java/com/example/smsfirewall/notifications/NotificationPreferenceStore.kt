package com.example.smsfirewall.notifications

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferenceStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // --- Ses ---
    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    // --- Titreşim ---
    fun isVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION, true)

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    // --- Sessiz Saatler ---
    fun isQuietHoursEnabled(): Boolean = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false)

    fun setQuietHoursEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply()
    }

    fun getQuietHoursStart(): Pair<Int, Int> {
        val hour = prefs.getInt(KEY_QUIET_START_HOUR, DEFAULT_QUIET_START_HOUR)
        val minute = prefs.getInt(KEY_QUIET_START_MINUTE, DEFAULT_QUIET_START_MINUTE)
        return hour to minute
    }

    fun setQuietHoursStart(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_QUIET_START_HOUR, hour)
            .putInt(KEY_QUIET_START_MINUTE, minute)
            .apply()
    }

    fun getQuietHoursEnd(): Pair<Int, Int> {
        val hour = prefs.getInt(KEY_QUIET_END_HOUR, DEFAULT_QUIET_END_HOUR)
        val minute = prefs.getInt(KEY_QUIET_END_MINUTE, DEFAULT_QUIET_END_MINUTE)
        return hour to minute
    }

    fun setQuietHoursEnd(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_QUIET_END_HOUR, hour)
            .putInt(KEY_QUIET_END_MINUTE, minute)
            .apply()
    }

    /**
     * Verilen saat:dakika değerinin sessiz saatler aralığında olup olmadığını kontrol eder.
     * Gece yarısını geçen aralıkları da destekler (ör. 22:00 - 07:00).
     */
    fun isInQuietHours(currentHour: Int, currentMinute: Int): Boolean {
        if (!isQuietHoursEnabled()) return false

        val (startH, startM) = getQuietHoursStart()
        val (endH, endM) = getQuietHoursEnd()

        val currentMinutes = currentHour * 60 + currentMinute
        val startMinutes = startH * 60 + startM
        val endMinutes = endH * 60 + endM

        return if (startMinutes <= endMinutes) {
            // Aynı gün içinde (ör. 08:00 - 18:00)
            currentMinutes in startMinutes until endMinutes
        } else {
            // Gece yarısını geçiyor (ör. 22:00 - 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    /**
     * Şu anki zamanın sessiz saatler aralığında olup olmadığını kontrol eder.
     */
    fun isCurrentlyInQuietHours(): Boolean {
        val cal = java.util.Calendar.getInstance()
        return isInQuietHours(
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE)
        )
    }

    companion object {
        private const val PREF_NAME = "notification_preferences"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_START_HOUR = "quiet_start_hour"
        private const val KEY_QUIET_START_MINUTE = "quiet_start_minute"
        private const val KEY_QUIET_END_HOUR = "quiet_end_hour"
        private const val KEY_QUIET_END_MINUTE = "quiet_end_minute"

        const val DEFAULT_QUIET_START_HOUR = 22
        const val DEFAULT_QUIET_START_MINUTE = 0
        const val DEFAULT_QUIET_END_HOUR = 7
        const val DEFAULT_QUIET_END_MINUTE = 0
    }
}
