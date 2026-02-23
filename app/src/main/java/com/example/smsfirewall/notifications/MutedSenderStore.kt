package com.example.smsfirewall.notifications

import android.content.Context
import java.util.Locale

class MutedSenderStore(context: Context) {
    private val appContext = context.applicationContext

    fun mute(sender: String) {
        val normalizedSender = normalizeSender(sender)
        if (normalizedSender.isBlank()) return

        val current = appContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_MUTED_SENDERS, emptySet())
            .orEmpty()
            .toMutableSet()
        current.add(normalizedSender)

        appContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_MUTED_SENDERS, current)
            .apply()
    }

    fun isMuted(sender: String): Boolean {
        val normalizedSender = normalizeSender(sender)
        if (normalizedSender.isBlank()) return false

        val current = appContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_MUTED_SENDERS, emptySet())
            .orEmpty()
        return current.contains(normalizedSender)
    }

    private fun normalizeSender(sender: String): String {
        return sender
            .trim()
            .lowercase(Locale.ROOT)
            .replace(NON_SIGNIFICANT_SENDER_CHARS_REGEX, "")
    }

    private companion object {
        const val PREF_NAME = "sms_sender_preferences"
        const val KEY_MUTED_SENDERS = "muted_senders"
        val NON_SIGNIFICANT_SENDER_CHARS_REGEX = Regex("[\\s()\\-]")
    }
}
