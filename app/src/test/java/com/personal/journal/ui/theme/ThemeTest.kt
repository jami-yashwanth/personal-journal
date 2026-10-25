package com.personal.journal.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {
    @Test
    fun colorsMatchSpec() {
        assertEquals(Color(0xFF121212), JournalColors.Background)
        assertEquals(Color(0xFF1C1C1C), JournalColors.Surface)
        assertEquals(Color(0xFF262626), JournalColors.SurfaceRaised)
        assertEquals(Color(0xFFEDEDED), JournalColors.PrimaryFill)
        assertEquals(Color(0xFF121212), JournalColors.OnPrimary)
        assertEquals(Color(0xFF8C8C8C), JournalColors.TextMuted)
    }
}
