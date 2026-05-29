package com.example.smsfirewall.ui.inbox

import com.example.smsfirewall.data.ConversationMetaStore
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.util.PHONE_COMPARE_LENGTH
import com.example.smsfirewall.util.normalizeSender
import java.util.Locale

internal fun buildConversations(
    messages: List<SmsEntity>,
    unreadIds: Set<Long> = emptySet(),
    unknownSenderLabel: String = "Bilinmeyen gönderici",
    metaStore: ConversationMetaStore? = null
): List<SmsConversation> {
    return messages
        .groupBy { normalizeSenderForGrouping(it.sender) }
        .mapNotNull { (senderKey, senderMessages) ->
            if (senderMessages.isEmpty()) {
                return@mapNotNull null
            }

            val sortedMessages = senderMessages.sortedBy { it.receivedAt }
            val latestMessage = sortedMessages.last()
            val unread = if (unreadIds.isEmpty()) 0 else {
                senderMessages.count { it.id in unreadIds }
            }

            val displaySender = latestMessage.sender.ifBlank { unknownSenderLabel }
            val isPinned = metaStore?.isPinned(displaySender) == true
            val isFavorite = metaStore?.isFavorite(displaySender) == true
            val isArchived = metaStore?.isArchived(displaySender) == true

            SmsConversation(
                senderKey = senderKey.ifBlank { "unknown_sender_${latestMessage.id}" },
                displaySender = displaySender,
                messages = sortedMessages,
                latestReceivedAt = latestMessage.receivedAt,
                unreadCount = unread,
                isPinned = isPinned,
                isFavorite = isFavorite,
                isArchived = isArchived
            )
        }
        .sortedWith(compareByDescending<SmsConversation> { it.isPinned }.thenByDescending { it.latestReceivedAt })
}

internal fun normalizeSenderForGrouping(sender: String): String {
    return normalizeSender(sender)
}

internal fun normalizeForSearch(value: String): String {
    return value
        .trim()
        .lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }
}

internal fun normalizeDigits(value: String): String {
    return value.filter { it.isDigit() }
}

internal fun numberLikeMatch(candidate: String, queryDigits: String): Boolean {
    if (queryDigits.isBlank()) return false

    val candidateDigits = normalizeDigits(candidate)
    if (candidateDigits.isBlank()) return false

    if (candidateDigits.contains(queryDigits) || queryDigits.contains(candidateDigits)) {
        return true
    }

    val candidateLastDigits = if (candidateDigits.length > PHONE_COMPARE_LENGTH) {
        candidateDigits.takeLast(PHONE_COMPARE_LENGTH)
    } else {
        candidateDigits
    }
    val queryLastDigits = if (queryDigits.length > PHONE_COMPARE_LENGTH) {
        queryDigits.takeLast(PHONE_COMPARE_LENGTH)
    } else {
        queryDigits
    }

    return candidateLastDigits.contains(queryLastDigits) || queryLastDigits.contains(candidateLastDigits)
}

internal fun SmsConversation.matchesSearchQuery(query: String): Boolean {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isBlank()) return true

    val normalizedQuery = normalizeForSearch(trimmedQuery)
    val queryDigits = normalizeDigits(trimmedQuery)

    if (matchesSenderDirectly(trimmedQuery, normalizedQuery, queryDigits)) {
        return true
    }

    return matchesMessageContent(trimmedQuery, normalizedQuery, queryDigits)
}

private fun SmsConversation.matchesSenderDirectly(
    trimmedQuery: String,
    normalizedQuery: String,
    queryDigits: String
): Boolean {
    if (displaySender.contains(trimmedQuery, ignoreCase = true)) return true
    if (normalizedQuery.isNotBlank() && normalizeForSearch(displaySender).contains(normalizedQuery)) return true
    if (numberLikeMatch(displaySender, queryDigits)) return true
    return false
}

private fun SmsConversation.matchesMessageContent(
    trimmedQuery: String,
    normalizedQuery: String,
    queryDigits: String
): Boolean {
    return messages.any { sms ->
        sms.body.contains(trimmedQuery, ignoreCase = true) ||
            (normalizedQuery.isNotBlank() && normalizeForSearch(sms.body).contains(normalizedQuery)) ||
            sms.sender.contains(trimmedQuery, ignoreCase = true) ||
            (normalizedQuery.isNotBlank() && normalizeForSearch(sms.sender).contains(normalizedQuery)) ||
            numberLikeMatch(sms.sender, queryDigits) ||
            numberLikeMatch(sms.body, queryDigits)
    }
}
