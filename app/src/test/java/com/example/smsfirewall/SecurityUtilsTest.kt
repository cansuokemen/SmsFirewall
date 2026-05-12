package com.example.smsfirewall

import com.example.smsfirewall.data.security.PinHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {
    @Test
    fun `pbkdf2 pin hash verifies matching pin`() {
        val salt = ByteArray(16) { it.toByte() }
        val hash = PinHasher.hash("123456", salt)

        assertTrue(PinHasher.verify("123456", salt, hash))
    }

    @Test
    fun `pbkdf2 pin hash rejects wrong pin`() {
        val salt = ByteArray(16) { (it * 3).toByte() }
        val hash = PinHasher.hash("123456", salt)

        assertFalse(PinHasher.verify("000000", salt, hash))
    }
}
