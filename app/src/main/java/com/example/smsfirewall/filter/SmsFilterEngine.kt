package com.example.smsfirewall.filter

import java.util.Locale

data class FilterDecision(
    val status: String,
    val reason: String
)

object SmsStatus {
    const val ALLOW = "ALLOW"
    const val BLOCK = "BLOCK"
    const val REVIEW = "REVIEW"
}

class SmsFilterEngine(
    private val blockedSenders: Set<String> = emptySet(),
    private val blockedKeywords: Set<String> = emptySet()
) {
    fun evaluate(sender: String, body: String): FilterDecision {
        val normalizedSender = sender.trim().lowercase(Locale.ROOT)
        val normalizedBody = body.lowercase(Locale.ROOT)

        if (normalizedSender.isNotEmpty() && blockedSenders.contains(normalizedSender)) {
            return FilterDecision(
                status = SmsStatus.BLOCK,
                reason = "Blocked sender"
            )
        }

        val matchedKeyword = blockedKeywords.firstOrNull { keyword ->
            keyword.isNotBlank() && normalizedBody.contains(keyword.lowercase(Locale.ROOT))
        }
        if (matchedKeyword != null) {
            return FilterDecision(
                status = SmsStatus.BLOCK,
                reason = "Blocked keyword: $matchedKeyword"
            )
        }

        return FilterDecision(
            status = SmsStatus.ALLOW,
            reason = "No rule matched"
        )
    }
}
