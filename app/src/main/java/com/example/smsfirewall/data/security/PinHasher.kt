package com.example.smsfirewall.data.security

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    const val ITERATIONS = 120_000
    const val KEY_LENGTH_BITS = 256

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    fun verify(pin: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        return MessageDigest.isEqual(hash(pin, salt), expectedHash)
    }
}
