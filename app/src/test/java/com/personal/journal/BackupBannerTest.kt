package com.personal.journal

import com.personal.journal.data.BackupStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupBannerTest {
    private val day = 86_400_000L

    @Test
    fun hiddenUnderThreeDays() = assertNull(backupBanner(true, BackupStatus(null, 0, "x", 0), 2 * day))

    @Test
    fun shownAfterThreeDays() =
        assertEquals("Backup has been failing for 4 days. Tap to fix.", backupBanner(true, BackupStatus(null, 0, "x", 0), 4 * day))

    @Test
    fun hiddenWhenDisconnectedOrHealthy() {
        assertNull(backupBanner(false, BackupStatus(null, 0, "x", 0), 9 * day))
        assertNull(backupBanner(true, BackupStatus(1, 3, null, null), 9 * day))
    }
}
