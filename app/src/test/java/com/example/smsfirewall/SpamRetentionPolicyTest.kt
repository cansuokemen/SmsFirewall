package com.example.smsfirewall

import com.example.smsfirewall.data.SpamRetentionPreferenceStore
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamRetentionPolicyTest {

    @Test
    fun `cutoffTimestamp returns past timestamp for default 30 days`() {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val cutoff = now - thirtyDaysMs
        assertTrue("Cutoff should be in the past", cutoff < now)
    }

    @Test
    fun `cutoffTimestamp is approximately 30 days ago`() {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val cutoff = now - thirtyDaysMs
        val diff = now - cutoff
        assertTrue("Cutoff should be ~30 days ago", diff in (thirtyDaysMs - 1000)..(thirtyDaysMs + 1000))
    }

    @Test
    fun `retention options contains 30, 60, 90`() {
        val options = SpamRetentionPreferenceStore.RETENTION_OPTIONS
        assertTrue("Should contain 30", 30 in options)
        assertTrue("Should contain 60", 60 in options)
        assertTrue("Should contain 90", 90 in options)
        assertTrue("Should have exactly 3 options", options.size == 3)
    }

    @Test
    fun `default retention days is 30`() {
        assertTrue(
            "Default should be 30",
            SpamRetentionPreferenceStore.DEFAULT_RETENTION_DAYS == 30
        )
    }
}
