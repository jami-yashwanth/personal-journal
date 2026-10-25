package com.personal.journal.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPolicyTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    @Test
    fun reminderSkippedWhenLoggedToday() = assertFalse(ReminderPolicy.shouldNotify(entriesToday = 2))

    @Test
    fun reminderPostsWhenNothingToday() = assertTrue(ReminderPolicy.shouldNotify(entriesToday = 0))

    @Test
    fun delayToLaterToday() {
        val now = ZonedDateTime.of(2026, 10, 25, 18, 0, 0, 0, ist)
        assertEquals(Duration.ofHours(3), ReminderPolicy.initialDelay(now, LocalTime.of(21, 0)))
    }

    @Test
    fun delayRollsToTomorrow() {
        val now = ZonedDateTime.of(2026, 10, 25, 22, 0, 0, 0, ist)
        assertEquals(Duration.ofHours(23), ReminderPolicy.initialDelay(now, LocalTime.of(21, 0)))
    }
}
