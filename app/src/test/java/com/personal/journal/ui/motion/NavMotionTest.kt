package com.personal.journal.ui.motion

import com.personal.journal.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class NavMotionTest {
    @Test fun captureAndMonthAreSheet() {
        assertEquals(NavMotion.Sheet, navMotion(Routes.CAPTURE, Routes.MONTH))
        assertEquals(NavMotion.Sheet, navMotion(Routes.MONTH, Routes.CAPTURE))
    }

    @Test fun monthDayAndDayEntryAreShared() {
        assertEquals(NavMotion.Shared, navMotion(Routes.MONTH, Routes.DAY))
        assertEquals(NavMotion.Shared, navMotion(Routes.DAY, Routes.MONTH))
        assertEquals(NavMotion.Shared, navMotion(Routes.DAY, Routes.ENTRY))
        assertEquals(NavMotion.Shared, navMotion(Routes.ENTRY, Routes.DAY))
    }

    @Test fun everythingElseIsPush() {
        listOf(
            Routes.MONTH to Routes.SETTINGS, Routes.SETTINGS to Routes.BACKUP_SETUP, Routes.CAPTURE to Routes.REVIEW,
            Routes.CAPTURE to Routes.WRITE, Routes.DAY to Routes.WRITE, Routes.MONTH to Routes.ENTRY, Routes.CAPTURE to Routes.DAY,
        ).forEach { (from, to) -> assertEquals("$from -> $to", NavMotion.Push, navMotion(from, to)) }
    }

    @Test fun unknownRoutesArePush() {
        assertEquals(NavMotion.Push, navMotion(null, Routes.MONTH))
        assertEquals(NavMotion.Push, navMotion("x", "y"))
    }
}
