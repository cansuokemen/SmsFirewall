package com.example.smsfirewall.notifications

import android.content.Context
import android.content.SharedPreferences
import com.example.smsfirewall.util.normalizeSender

class MutedSenderStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun mute(sender: String) {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return

        val current = getMutedSenders().toMutableSet()
        if (current.none { normalizeSender(it) == normalized }) {
            current.add(normalized)
        }

        prefs.edit().putStringSet(KEY_MUTED_SENDERS, current).apply()
    }

    fun isMuted(sender: String): Boolean {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return false

        return getMutedSenders().any { normalizeSender(it) == normalized }
    }

    fun unmute(sender: String) {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return

        val current = getMutedSenders().toMutableSet()
        if (current.removeAll { normalizeSender(it) == normalized }) {
            prefs.edit().putStringSet(KEY_MUTED_SENDERS, current).apply()
        }
    }

    private fun getMutedSenders(): Set<String> {
        return prefs.getStringSet(KEY_MUTED_SENDERS, emptySet()).orEmpty()
    }

    private companion object {
        const val PREF_NAME = "sms_sender_preferences"
        const val KEY_MUTED_SENDERS = "muted_senders"
    }
}
