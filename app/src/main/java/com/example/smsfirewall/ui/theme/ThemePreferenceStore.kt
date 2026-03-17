package com.example.smsfirewall.ui.theme

import android.content.Context

/**
 * Kullanıcının tema tercihini SharedPreferences ile saklayan store.
 * Üç mod destekler: SYSTEM (varsayılan), LIGHT, DARK.
 */
class ThemePreferenceStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val value = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(value ?: ThemeMode.SYSTEM.name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "theme_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
