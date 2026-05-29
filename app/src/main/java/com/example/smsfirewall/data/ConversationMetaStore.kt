package com.example.smsfirewall.data

import android.content.Context
import android.content.SharedPreferences
import com.example.smsfirewall.util.normalizeSender

class ConversationMetaStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isPinned(sender: String): Boolean = contains(KEY_PINNED, sender)
    fun isFavorite(sender: String): Boolean = contains(KEY_FAVORITE, sender)
    fun isArchived(sender: String): Boolean = contains(KEY_ARCHIVED, sender)

    fun setPinned(sender: String, pinned: Boolean) = setFlag(KEY_PINNED, sender, pinned)
    fun setFavorite(sender: String, favorite: Boolean) = setFlag(KEY_FAVORITE, sender, favorite)
    fun setArchived(sender: String, archived: Boolean) = setFlag(KEY_ARCHIVED, sender, archived)

    fun pinnedSenders(): Set<String> = prefs.getStringSet(KEY_PINNED, emptySet()).orEmpty()
    fun favoriteSenders(): Set<String> = prefs.getStringSet(KEY_FAVORITE, emptySet()).orEmpty()
    fun archivedSenders(): Set<String> = prefs.getStringSet(KEY_ARCHIVED, emptySet()).orEmpty()

    private fun contains(key: String, sender: String): Boolean {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return false
        return prefs.getStringSet(key, emptySet()).orEmpty().contains(normalized)
    }

    private fun setFlag(key: String, sender: String, value: Boolean) {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return
        val current = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        val changed = if (value) current.add(normalized) else current.remove(normalized)
        if (changed) prefs.edit().putStringSet(key, current).apply()
    }

    private companion object {
        const val PREF_NAME = "sms_conversation_meta"
        const val KEY_PINNED = "pinned_senders"
        const val KEY_FAVORITE = "favorite_senders"
        const val KEY_ARCHIVED = "archived_senders"
    }
}
