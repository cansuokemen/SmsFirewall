package com.example.smsfirewall

import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.ui.inbox.buildConversations
import com.example.smsfirewall.ui.inbox.normalizeForSearch
import com.example.smsfirewall.ui.inbox.normalizeSenderForGrouping
import com.example.smsfirewall.ui.inbox.normalizeDigits
import com.example.smsfirewall.ui.inbox.numberLikeMatch
import com.example.smsfirewall.ui.inbox.formatMessageTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsUtilsTest {

    // ─── normalizeSenderForGrouping ─────────────────────────────

    @Test
    fun `normalizeSenderForGrouping trims and lowercases`() {
        assertEquals("abc", normalizeSenderForGrouping("  ABC  "))
    }

    @Test
    fun `normalizeSenderForGrouping strips non-alphanumeric`() {
        assertEquals("5551234567", normalizeSenderForGrouping("+555-123-4567"))
    }

    @Test
    fun `normalizeSenderForGrouping takes last 10 digits for long numbers`() {
        assertEquals("5551234567", normalizeSenderForGrouping("+905551234567"))
    }

    @Test
    fun `normalizeSenderForGrouping keeps short numbers as-is`() {
        assertEquals("12345", normalizeSenderForGrouping("12345"))
    }

    @Test
    fun `normalizeSenderForGrouping keeps alpha senders lowercase`() {
        assertEquals("bankasi", normalizeSenderForGrouping("BANKASI"))
    }

    @Test
    fun `normalizeSenderForGrouping returns empty for blank`() {
        assertEquals("", normalizeSenderForGrouping("  "))
    }

    // ─── normalizeForSearch ─────────────────────────────────────

    @Test
    fun `normalizeForSearch strips special chars`() {
        assertEquals("merhaba123", normalizeForSearch("Merhaba 123!"))
    }

    @Test
    fun `normalizeForSearch handles empty string`() {
        assertEquals("", normalizeForSearch(""))
    }

    // ─── normalizeDigits ────────────────────────────────────────

    @Test
    fun `normalizeDigits extracts only digits`() {
        assertEquals("905551234567", normalizeDigits("+90 555 123 45 67"))
    }

    @Test
    fun `normalizeDigits returns empty for no digits`() {
        assertEquals("", normalizeDigits("abc"))
    }

    // ─── numberLikeMatch ────────────────────────────────────────

    @Test
    fun `numberLikeMatch matches substring`() {
        assertTrue(numberLikeMatch("+905551234567", "5551234567"))
    }

    @Test
    fun `numberLikeMatch matches last 10 digits`() {
        assertTrue(numberLikeMatch("+905551234567", "+905551234567"))
    }

    @Test
    fun `numberLikeMatch returns false for blank query`() {
        assertFalse(numberLikeMatch("+905551234567", ""))
    }

    @Test
    fun `numberLikeMatch returns false for non-matching`() {
        assertFalse(numberLikeMatch("+905551234567", "9999999999"))
    }

    // ─── buildConversations ─────────────────────────────────────

    @Test
    fun `buildConversations groups messages by sender`() {
        val messages = listOf(
            createSms(1, "+905551111111", "Hello", 1000L),
            createSms(2, "+905551111111", "World", 2000L),
            createSms(3, "+905552222222", "Hi", 3000L)
        )
        val conversations = buildConversations(messages)
        assertEquals(2, conversations.size)
    }

    @Test
    fun `buildConversations sorts by latest message`() {
        val messages = listOf(
            createSms(1, "+905551111111", "Old", 1000L),
            createSms(2, "+905552222222", "New", 5000L)
        )
        val conversations = buildConversations(messages)
        assertEquals("+905552222222", conversations.first().displaySender)
    }

    @Test
    fun `buildConversations calculates unread count`() {
        val messages = listOf(
            createSms(1, "+905551111111", "Msg1", 1000L),
            createSms(2, "+905551111111", "Msg2", 2000L),
            createSms(3, "+905551111111", "Msg3", 3000L)
        )
        val unreadIds = setOf(1L, 3L)
        val conversations = buildConversations(messages, unreadIds)
        assertEquals(2, conversations.first().unreadCount)
    }

    @Test
    fun `buildConversations handles empty list`() {
        val conversations = buildConversations(emptyList())
        assertTrue(conversations.isEmpty())
    }

    @Test
    fun `buildConversations normalizes sender for grouping`() {
        val messages = listOf(
            createSms(1, "+90 555 111 1111", "A", 1000L),
            createSms(2, "+905551111111", "B", 2000L)
        )
        val conversations = buildConversations(messages)
        assertEquals(1, conversations.size)
        assertEquals(2, conversations.first().messages.size)
    }

    // ─── formatTimestamps ───────────────────────────────────────

    @Test
    fun `formatMessageTimestamp returns HH-mm format`() {
        // A known timestamp - just verify it returns non-empty
        val result = formatMessageTimestamp(System.currentTimeMillis())
        assertTrue(result.isNotBlank())
        assertTrue(result.contains(":"))
    }

    // formatConversationTimestamp requires Context, tested via instrumented tests

    // ─── Helper ─────────────────────────────────────────────────

    private fun createSms(
        id: Long,
        sender: String,
        body: String,
        receivedAt: Long
    ) = SmsEntity(
        id = id,
        sender = sender,
        body = body,
        receivedAt = receivedAt,
        status = SmsStatus.ALLOW,
        reason = "Test"
    )
}
