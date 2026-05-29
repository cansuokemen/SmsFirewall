package com.example.smsfirewall.data

import android.content.Context
import com.example.smsfirewall.data.security.CryptoBox
import com.example.smsfirewall.data.security.PinHasher
import java.security.MessageDigest
import java.security.SecureRandom

class AppLockPreferenceStore(
    context: Context,
    private val cryptoBox: CryptoBox
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun isLockEnabled(): Boolean = prefs.getBoolean(KEY_LOCK_ENABLED, false) && hasPin()

    fun hasPin(): Boolean =
        !readProtectedString(KEY_PIN_HASH, null).isNullOrBlank() ||
            !prefs.getString(KEY_LEGACY_PIN_HASH, null).isNullOrBlank()

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) && isLockEnabled()

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled && isLockEnabled()).apply()
    }

    fun setPin(pin: String) {
        val salt = ByteArray(SALT_SIZE)
        secureRandom.nextBytes(salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, cryptoBox.encrypt(salt.toBase64()))
            .putString(KEY_PIN_HASH, cryptoBox.encrypt(PinHasher.hash(pin, salt).toBase64()))
            .putBoolean(KEY_LOCK_ENABLED, true)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val now = System.currentTimeMillis()
        if (prefs.getLong(KEY_LOCKED_UNTIL, 0L) > now) return false

        val verified = verifyProtectedPin(pin) || verifyLegacyPin(pin)
        if (verified) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKED_UNTIL, 0L)
                .apply()
            if (readProtectedString(KEY_PIN_HASH, null).isNullOrBlank()) {
                setPin(pin)
            }
            return true
        }

        recordFailedAttempt(now)
        return false
    }

    fun clearLock() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .remove(KEY_LEGACY_PIN_SALT)
            .remove(KEY_LEGACY_PIN_HASH)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
    }

    private fun verifyProtectedPin(pin: String): Boolean {
        val salt = readProtectedString(KEY_PIN_SALT, null)?.fromBase64() ?: return false
        val storedHash = readProtectedString(KEY_PIN_HASH, null)?.fromBase64() ?: return false
        return PinHasher.verify(pin, salt, storedHash)
    }

    private fun verifyLegacyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_LEGACY_PIN_SALT, null)?.fromBase64() ?: return false
        val storedHash = prefs.getString(KEY_LEGACY_PIN_HASH, null) ?: return false
        return MessageDigest.isEqual(legacyHashPin(pin, salt).toByteArray(), storedHash.toByteArray())
    }

    private fun recordFailedAttempt(now: Long) {
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val lockedUntil = if (attempts >= MAX_FAILED_ATTEMPTS) {
            now + LOCKOUT_MS
        } else {
            0L
        }
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, attempts)
            .putLong(KEY_LOCKED_UNTIL, lockedUntil)
            .apply()
    }

    private fun readProtectedString(key: String, fallback: String?): String? {
        val value = prefs.getString(key, null) ?: return fallback
        return cryptoBox.decrypt(value).ifBlank { fallback }
    }

    private fun legacyHashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest().toBase64()
    }

    private fun ByteArray.toBase64(): String = java.util.Base64.getEncoder().encodeToString(this)

    private fun String.fromBase64(): ByteArray = java.util.Base64.getDecoder().decode(this)

    companion object {
        private const val PREF_NAME = "app_lock_preferences"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_LEGACY_PIN_SALT = "pin_salt"
        private const val KEY_LEGACY_PIN_HASH = "pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKED_UNTIL = "locked_until"
        private const val SALT_SIZE = 16
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MS = 30_000L
    }
}
