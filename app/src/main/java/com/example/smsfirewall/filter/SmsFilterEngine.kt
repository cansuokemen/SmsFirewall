package com.example.smsfirewall.filter

import com.example.smsfirewall.util.normalizeSender
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
    private val blockedKeywords: Set<String> = emptySet(),
    private val classifier: SpamClassifier? = null
) {
    fun evaluate(sender: String, body: String): FilterDecision {
        val normalizedSender = normalizeSender(sender)

        // 1. Engellenen gönderen — en hızlı kontrol
        if (normalizedSender.isNotEmpty() &&
            blockedSenders.any { normalizeSender(it) == normalizedSender }
        ) {
            return FilterDecision(status = SmsStatus.BLOCK, reason = "Blocked sender")
        }

        // 2. AI modeli — %97 doğruluk
        if (classifier != null) {
            val score = classifier.spamScore(body)
            if (score >= 0.5f) {
                return FilterDecision(
                    status = SmsStatus.BLOCK,
                    reason  = "AI model: spam score %.2f".format(score)
                )
            }
        }

        // 3. Anahtar kelime filtresi — yedek güvence
        val normalizedBody = body.lowercase(Locale.ROOT)
        val matchedKeyword = blockedKeywords.firstOrNull { keyword ->
            keyword.isNotBlank() && normalizedBody.contains(keyword)
        }
        if (matchedKeyword != null) {
            return FilterDecision(status = SmsStatus.BLOCK, reason = "Blocked keyword: $matchedKeyword")
        }

        return FilterDecision(status = SmsStatus.ALLOW, reason = "No rule matched")
    }
}
