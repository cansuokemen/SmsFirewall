package com.example.smsfirewall.filter

import android.content.Context
import com.example.smsfirewall.util.normalizeSender
import java.util.Locale

/**
 * SharedPreferences tabanlı filtre anahtar kelime deposu.
 * Varsayılan engelleme anahtar kelimeleri ilk çalıştırmada otomatik yüklenir.
 * Kullanıcı bu kelimeleri ileride yapılandırabilir.
 */
class FilterKeywordStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getBlockedKeywords(): Set<String> {
        if (!prefs.contains(KEY_BLOCKED_KEYWORDS)) {
            prefs.edit()
                .putStringSet(KEY_BLOCKED_KEYWORDS, DEFAULT_KEYWORDS)
                .apply()
        }
        return prefs.getStringSet(KEY_BLOCKED_KEYWORDS, DEFAULT_KEYWORDS).orEmpty()
    }

    fun addKeyword(keyword: String) {
        val normalized = keyword.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return

        val current = getBlockedKeywords().toMutableSet()
        current.add(normalized)
        prefs.edit()
            .putStringSet(KEY_BLOCKED_KEYWORDS, current)
            .apply()
    }

    fun removeKeyword(keyword: String) {
        val normalized = keyword.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return

        val current = getBlockedKeywords().toMutableSet()
        if (current.remove(normalized)) {
            prefs.edit()
                .putStringSet(KEY_BLOCKED_KEYWORDS, current)
                .apply()
        }
    }

    fun getBlockedSenders(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_SENDERS, emptySet()).orEmpty()
    }

    fun addBlockedSender(sender: String) {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return

        val current = getBlockedSenders().toMutableSet()
        if (current.none { normalizeSender(it) == normalized }) {
            current.add(normalized)
            prefs.edit()
                .putStringSet(KEY_BLOCKED_SENDERS, current)
                .apply()
        }
    }

    fun removeBlockedSender(sender: String) {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return

        val current = getBlockedSenders().toMutableSet()
        if (current.removeAll { normalizeSender(it) == normalized }) {
            prefs.edit()
                .putStringSet(KEY_BLOCKED_SENDERS, current)
                .apply()
        }
    }

    companion object {
        private const val PREF_NAME = "sms_filter_settings"
        private const val KEY_BLOCKED_KEYWORDS = "blocked_keywords"
        private const val KEY_BLOCKED_SENDERS = "blocked_senders"

        val DEFAULT_KEYWORDS = setOf(
            "free", "winner", "loan", "debt", "credit",
            "prize", "congratulations", "urgent", "act now",
            "kazandınız", "kredi", "borç", "tebrikler"
        )
    }
}
