package com.personal.journal.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagColoursTest {
    @Test
    fun sameNameSameColour() {
        assertEquals(tagColour("gym"), tagColour("gym"))
        assertEquals(tagColour("gym"), tagColour(" Gym "))
    }

    @Test
    fun alwaysInPalette() {
        repeat(200) { assertTrue(tagColour("tag$it") in TagPalette) }
    }

    @Test
    fun firstTagAlphabetically() {
        assertEquals(tagColour("cricket"), entryColour(listOf("office", "cricket")))
    }

    @Test
    fun noTagsIsPink() {
        assertEquals(Untagged, entryColour(emptyList()))
    }

    @Test
    fun tealHasWhiteText() {
        val teal = TagPalette.first { it.fill == Color(0xFF2E8A78) }
        assertEquals(Color.White, teal.onFill)
    }
}
