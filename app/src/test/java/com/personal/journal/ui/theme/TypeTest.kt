package com.personal.journal.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeTest {
    @Test
    fun displayIsOutfitBold42() {
        val display = JournalTypography.displayLarge
        assertEquals(42.sp, display.fontSize)
        assertEquals(FontWeight.Bold, display.fontWeight)
        assertEquals(Outfit, display.fontFamily)
    }

    @Test
    fun slipStyleScales() {
        val style = slipStyle(13.sp)
        assertEquals(13.sp, style.fontSize)
        assertEquals(13.sp * 1.2f, style.lineHeight)
    }
}
