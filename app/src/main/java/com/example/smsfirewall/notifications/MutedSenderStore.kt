package com.example.smsfirewall.notifications

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

class MutedSenderStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun mute(sender: String) {
        val normalizedSender = normalizeSender(sender)
        if (normalizedSender.isBlank()) return

        val current = getMutedSenders().toMutableSet()
        if (current.none { normalizeSender(it) == normalizedSender }) {
            current.add(normalizedSender)
        }

        prefs.edit().putStringSet(KEY_MUTED_SENDERS, current).apply()
    }

    fun isMuted(sender: String): Boolean {
        val normalizedSender = normalizeSender(sender)
        if (normalizedSender.isBlank()) return false

        return getMutedSenders().any { normalizeSender(it) == normalizedSender }
    }

    fun unmute(sender: String) {
        val normalizedSender = normalizeSender(sender)
        if (normalizedSender.isBlank()) return

        val current = getMutedSenders().toMutableSet()
        if (current.removeAll { normalizeSender(it) == normalizedSender }) {
            prefs.edit().putStringSet(KEY_MUTED_SENDERS, current).apply()
        }
    }

    private fun getMutedSenders(): Set<String> {
        return prefs.getStringSet(KEY_MUTED_SENDERS, emptySet()).orEmpty()
    }

    private fun normalizeSender(sender: String): String {
        val alphanumeric = sender
            .trim()
            .lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() }
        if (alphanumeric.isBlank()) return ""

        return if (alphanumeric.all { it.isDigit() }) {
            if (alphanumeric.length > PHONE_COMPARE_LENGTH) {
                alphanumeric.takeLast(PHONE_COMPARE_LENGTH)
            } else {
                alphanumeric
            }
        } else {
            alphanumeric
        }
    }

    private companion object {
        const val PREF_NAME = "sms_sender_preferences"
        const val KEY_MUTED_SENDERS = "muted_senders"
        const val PHONE_COMPARE_LENGTH = 10
    }
}
