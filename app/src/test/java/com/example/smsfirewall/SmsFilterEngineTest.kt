package com.example.smsfirewall

import com.example.smsfirewall.filter.SmsFilterEngine
import com.example.smsfirewall.filter.SmsStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsFilterEngineTest {

    @Test
    fun `allow message when no rules match`() {
        val engine = SmsFilterEngine(
            blockedSenders = setOf("spammer"),
            blockedKeywords = setOf("free")
        )
        val result = engine.evaluate(sender = "+905551234567", body = "Merhaba, nasılsın?")
        assertEquals(SmsStatus.ALLOW, result.status)
    }

    @Test
    fun `block message when sender is blocked`() {
        val engine = SmsFilterEngine(
            blockedSenders = setOf("spammer"),
            blockedKeywords = emptySet()
        )
        val result = engine.evaluate(sender = "SPAMMER", body = "Hello")
        assertEquals(SmsStatus.BLOCK, result.status)
    }

    @Test
    fun `block message when keyword matches`() {
        val engine = SmsFilterEngine(
            blockedSenders = emptySet(),
            blockedKeywords = setOf("free", "winner")
        )
        val result = engine.evaluate(sender = "+905551234567", body = "You are the WINNER!")
        assertEquals(SmsStatus.BLOCK, result.status)
    }

    @Test
    fun `keyword matching is case insensitive`() {
        val engine = SmsFilterEngine(
            blockedKeywords = setOf("credit")
        )
        val result = engine.evaluate(sender = "Bank", body = "Your CREDIT card is ready")
        assertEquals(SmsStatus.BLOCK, result.status)
    }

    @Test
    fun `sender matching is case insensitive`() {
        val engine = SmsFilterEngine(
            blockedSenders = setOf("badguy")
        )
        val result = engine.evaluate(sender = "BADGUY", body = "Normal message")
        assertEquals(SmsStatus.BLOCK, result.status)
    }

    @Test
    fun `allow message with empty filter sets`() {
        val engine = SmsFilterEngine()
        val result = engine.evaluate(sender = "Anyone", body = "free winner loan")
        assertEquals(SmsStatus.ALLOW, result.status)
    }

    @Test
    fun `block reason contains matched keyword`() {
        val engine = SmsFilterEngine(
            blockedKeywords = setOf("loan")
        )
        val result = engine.evaluate(sender = "+905551234567", body = "Get a loan today!")
        assertEquals(SmsStatus.BLOCK, result.status)
        assert(result.reason.contains("loan", ignoreCase = true))
    }

    @Test
    fun `blocked sender takes priority over keyword check`() {
        val engine = SmsFilterEngine(
            blockedSenders = setOf("spamsender"),
            blockedKeywords = setOf("free")
        )
        val result = engine.evaluate(sender = "SpamSender", body = "No keywords here")
        assertEquals(SmsStatus.BLOCK, result.status)
        assert(result.reason.contains("sender", ignoreCase = true))
    }

    @Test
    fun `empty body does not crash`() {
        val engine = SmsFilterEngine(
            blockedKeywords = setOf("test")
        )
        val result = engine.evaluate(sender = "+905551234567", body = "")
        assertEquals(SmsStatus.ALLOW, result.status)
    }

    @Test
    fun `empty sender does not crash`() {
        val engine = SmsFilterEngine(
            blockedSenders = setOf("blocked")
        )
        val result = engine.evaluate(sender = "", body = "Hello")
        assertEquals(SmsStatus.ALLOW, result.status)
    }

    @Test
    fun `blank keyword in set is ignored`() {
        val engine = SmsFilterEngine(
            blockedKeywords = setOf("", "  ", "valid")
        )
        val result = engine.evaluate(sender = "Someone", body = "This has valid keyword")
        assertEquals(SmsStatus.BLOCK, result.status)
    }
}
