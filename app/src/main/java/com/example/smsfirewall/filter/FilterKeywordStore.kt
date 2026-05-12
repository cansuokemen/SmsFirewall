package com.example.smsfirewall.filter

import android.content.Context
import com.example.smsfirewall.data.security.CryptoBox
import com.example.smsfirewall.util.normalizeSender
import java.util.Locale

/**
 * SharedPreferences tabanlı filtre anahtar kelime deposu.
 * Varsayılan engelleme anahtar kelimeleri ilk çalıştırmada otomatik yüklenir.
 * Kullanıcı bu kelimeleri ileride yapılandırabilir.
 */
class FilterKeywordStore(
    context: Context,
    private val cryptoBox: CryptoBox
) {
    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getBlockedKeywords(): Set<String> {
        if (!prefs.contains(KEY_BLOCKED_KEYWORDS)) {
            putEncryptedStringSet(KEY_BLOCKED_KEYWORDS, DEFAULT_KEYWORDS)
        }
        return getEncryptedStringSet(KEY_BLOCKED_KEYWORDS, DEFAULT_KEYWORDS)
    }

    fun addKeyword(keyword: String) {
        val normalized = keyword.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return

        val current = getBlockedKeywords().toMutableSet()
        current.add(normalized)
        putEncryptedStringSet(KEY_BLOCKED_KEYWORDS, current)
    }

    fun removeKeyword(keyword: String) {
        val normalized = keyword.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return

        val current = getBlockedKeywords().toMutableSet()
        if (current.remove(normalized)) {
            putEncryptedStringSet(KEY_BLOCKED_KEYWORDS, current)
        }
    }

    fun getBlockedSenders(): Set<String> {
        return getEncryptedStringSet(KEY_BLOCKED_SENDERS, emptySet())
    }

    fun addBlockedSender(sender: String) {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return

        val current = getBlockedSenders().toMutableSet()
        if (current.none { normalizeSender(it) == normalized }) {
            current.add(normalized)
            putEncryptedStringSet(KEY_BLOCKED_SENDERS, current)
        }
    }

    fun removeBlockedSender(sender: String) {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return

        val current = getBlockedSenders().toMutableSet()
        if (current.removeAll { normalizeSender(it) == normalized }) {
            putEncryptedStringSet(KEY_BLOCKED_SENDERS, current)
        }
    }

    private fun getEncryptedStringSet(key: String, defaultValue: Set<String>): Set<String> {
        return when (val raw = prefs.all[key]) {
            is String -> cryptoBox.decrypt(raw)
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            is Set<*> -> raw.filterIsInstance<String>().toSet().also {
                putEncryptedStringSet(key, it)
            }
            else -> defaultValue
        }
    }

    private fun putEncryptedStringSet(key: String, values: Set<String>) {
        prefs.edit()
            .putString(key, cryptoBox.encrypt(values.sorted().joinToString(separator = "\n")))
            .apply()
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
