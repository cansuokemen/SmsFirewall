package com.example.smsfirewall

import com.example.smsfirewall.data.SpamRetentionPolicy
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamRetentionPolicyTest {

    @Test
    fun `cutoffTimestamp returns past timestamp`() {
        val cutoff = SpamRetentionPolicy.cutoffTimestamp()
        val now = System.currentTimeMillis()
        assertTrue("Cutoff should be in the past", cutoff < now)
    }

    @Test
    fun `cutoffTimestamp is approximately 30 days ago`() {
        val cutoff = SpamRetentionPolicy.cutoffTimestamp()
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val diff = now - cutoff
        // Allow 1 second tolerance
        assertTrue("Cutoff should be ~30 days ago", diff in (thirtyDaysMs - 1000)..(thirtyDaysMs + 1000))
    }
}
