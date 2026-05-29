package com.example.smsfirewall.data

import android.content.Context

enum class ListDensity { COMFORTABLE, COMPACT }

enum class AppTextSize { SMALL, NORMAL, LARGE }

enum class BubbleStyle { MODERN, SOFT }

class AppearancePreferenceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getListDensity(): ListDensity = readEnum(KEY_LIST_DENSITY, ListDensity.COMFORTABLE)

    fun setListDensity(value: ListDensity) {
        prefs.edit().putString(KEY_LIST_DENSITY, value.name).apply()
    }

    fun getTextSize(): AppTextSize = readEnum(KEY_TEXT_SIZE, AppTextSize.NORMAL)

    fun setTextSize(value: AppTextSize) {
        prefs.edit().putString(KEY_TEXT_SIZE, value.name).apply()
    }

    fun getBubbleStyle(): BubbleStyle = readEnum(KEY_BUBBLE_STYLE, BubbleStyle.MODERN)

    fun setBubbleStyle(value: BubbleStyle) {
        prefs.edit().putString(KEY_BUBBLE_STYLE, value.name).apply()
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, fallback: T): T {
        val stored = prefs.getString(key, fallback.name) ?: fallback.name
        return runCatching { enumValueOf<T>(stored) }.getOrDefault(fallback)
    }

    companion object {
        private const val PREF_NAME = "appearance_preferences"
        private const val KEY_LIST_DENSITY = "list_density"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_BUBBLE_STYLE = "bubble_style"
    }
}
