package com.personal.journal.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun paletteValuesMatchSpec() {
        assertEquals(Color(0xFFFFF6E9), LightPalette.background)
        assertEquals(Color(0xFF191614), DarkPalette.background)
        assertEquals(Color(0xFF1E1B19), LightPalette.action)
        assertEquals(Color(0xFFF3ECE3), DarkPalette.action)
        assertEquals(Color(0xFFF4E9DA), LightPalette.sunk)
        assertEquals(Color(0xFFEFE8DE), DarkPalette.paper)
        assertEquals(Color(0xFF1E1B19), LightPalette.onColour)
        assertEquals(Color(0xFF1E1B19), DarkPalette.onColour)
    }

    @Test
    fun darkThemeProvidesDarkPalette() {
        var isDark = false
        rule.setContent { JournalTheme(darkTheme = true) { isDark = LocalJournalPalette.current.isDark } }
        rule.waitForIdle()
        assertTrue(isDark)
    }

    @Test
    fun lightThemeProvidesLightPalette() {
        var isDark = true
        rule.setContent { JournalTheme(darkTheme = false) { isDark = LocalJournalPalette.current.isDark } }
        rule.waitForIdle()
        assertFalse(isDark)
    }
}
