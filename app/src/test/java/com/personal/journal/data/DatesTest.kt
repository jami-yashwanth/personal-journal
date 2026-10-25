package com.personal.journal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneId

class DatesTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    @Test
    fun lateNightStaysOnDay() {
        val millis = OffsetDateTime.parse("2026-10-25T23:59:00+05:30").toInstant().toEpochMilli()
        assertEquals("2026-10-25", localDateOf(millis, ist))
        // The same instant in UTC is still the 25th here, but 00:30 IST would not be:
        val justAfter = OffsetDateTime.parse("2026-10-26T00:30:00+05:30").toInstant().toEpochMilli()
        assertEquals("2026-10-26", localDateOf(justAfter, ist))
        assertEquals("2026-10-25", localDateOf(justAfter, ZoneId.of("UTC")))
    }

    @Test
    fun exifParsing() {
        val expected = OffsetDateTime.parse("2026-10-23T19:05:00+05:30").toInstant().toEpochMilli()
        assertEquals(expected, exifDateToMillis("2026:10:23 19:05:00", ist))
        assertNull(exifDateToMillis(null, ist))
        assertNull(exifDateToMillis("", ist))
        assertNull(exifDateToMillis("0000:00:00 00:00:00", ist))
        assertNull(exifDateToMillis("garbage", ist))
    }
}
