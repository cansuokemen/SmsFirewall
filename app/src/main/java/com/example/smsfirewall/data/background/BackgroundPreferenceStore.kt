package com.example.smsfirewall.data.background

import android.content.Context
import com.example.smsfirewall.util.normalizeSender

/**
 * Ana menü ve sohbete-özel arka plan tercihlerini saklar.
 *
 * - Ana menü tercihi tek bir SharedPreferences anahtarında tutulur.
 * - Sohbet tercihleri ayrı bir SharedPreferences dosyasında, normalize
 *   edilmiş gönderen adresine göre key'lenir; eksikse ana menü
 *   tercihine geri düşer.
 */
class BackgroundPreferenceStore(context: Context) {

    private val appContext = context.applicationContext

    private val mainPrefs get() =
        appContext.getSharedPreferences(PREF_MAIN, Context.MODE_PRIVATE)

    private val convPrefs get() =
        appContext.getSharedPreferences(PREF_PER_CONV, Context.MODE_PRIVATE)

    fun getMainBackground(): BackgroundSpec =
        BackgroundSpec.deserialize(mainPrefs.getString(KEY_MAIN, null))

    fun setMainBackground(spec: BackgroundSpec) {
        mainPrefs.edit().putString(KEY_MAIN, spec.serialize()).apply()
    }

    fun resetMainBackground() {
        mainPrefs.edit().remove(KEY_MAIN).apply()
    }

    fun getConversationBackground(address: String): BackgroundSpec? {
        val key = normalizeSender(address)
        if (key.isBlank()) return null
        val raw = convPrefs.getString(key, null) ?: return null
        return BackgroundSpec.deserialize(raw)
    }

    fun setConversationBackground(address: String, spec: BackgroundSpec) {
        val key = normalizeSender(address)
        if (key.isBlank()) return
        convPrefs.edit().putString(key, spec.serialize()).apply()
    }

    fun resetConversationBackground(address: String) {
        val key = normalizeSender(address)
        if (key.isBlank()) return
        convPrefs.edit().remove(key).apply()
    }

    /** Bir CustomImage dosya adının başka bir tercih tarafından
     *  kullanılıp kullanılmadığını döner — temizlik için. */
    fun isImageInUse(fileName: String): Boolean {
        val mainSpec = getMainBackground()
        if (mainSpec is BackgroundSpec.CustomImage && mainSpec.fileName == fileName) return true
        val all = convPrefs.all
        for ((_, value) in all) {
            val raw = value as? String ?: continue
            val spec = BackgroundSpec.deserialize(raw)
            if (spec is BackgroundSpec.CustomImage && spec.fileName == fileName) return true
        }
        return false
    }

    companion object {
        private const val PREF_MAIN     = "background_main_prefs"
        private const val PREF_PER_CONV = "background_per_conv_prefs"
        private const val KEY_MAIN      = "main_background"
    }
}
