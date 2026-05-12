package com.example.smsfirewall.notifications

import android.content.Context
import android.content.SharedPreferences
import com.example.smsfirewall.data.security.CryptoBox
import com.example.smsfirewall.util.normalizeSender

class MutedSenderStore(
    context: Context,
    private val cryptoBox: CryptoBox
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun mute(sender: String) {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return

        val current = getMutedSenders().toMutableSet()
        if (current.none { normalizeSender(it) == normalized }) {
            current.add(normalized)
        }

        putMutedSenders(current)
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
            putMutedSenders(current)
        }
    }

    private fun getMutedSenders(): Set<String> {
        return when (val raw = prefs.all[KEY_MUTED_SENDERS]) {
            is String -> cryptoBox.decrypt(raw)
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            is Set<*> -> raw.filterIsInstance<String>().toSet().also { putMutedSenders(it) }
            else -> emptySet()
        }
    }

    private fun putMutedSenders(values: Set<String>) {
        prefs.edit()
            .putString(KEY_MUTED_SENDERS, cryptoBox.encrypt(values.sorted().joinToString(separator = "\n")))
            .apply()
    }

    private companion object {
        const val PREF_NAME = "sms_sender_preferences"
        const val KEY_MUTED_SENDERS = "muted_senders"
    }
}
