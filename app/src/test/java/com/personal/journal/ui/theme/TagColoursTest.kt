package com.personal.journal.ui.theme

import androidx.compose.ui.graphics.Color
import com.personal.journal.data.TAG_COLOUR_SLOTS
import com.personal.journal.data.db.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class TagColoursTest {
    private fun tag(name: String, slot: Int) = TagEntity(name, name, useCount = 1, lastUsedAt = 1, colourSlot = slot)

    @Test
    fun paletteHasOneColourPerSlot() {
        assertEquals(TAG_COLOUR_SLOTS, TagPalette.size)
        assertEquals(TAG_COLOUR_SLOTS, TagPalette.toSet().size)
    }

    /** Pins slot -> colour: tags store their slot, so reordering TagPalette would recolour every existing tag. */
    @Test
    fun slotTwoStaysLavender() {
        assertEquals(Color(0xFFB8B3F6), tagColour(2).fill)
    }

    @Test
    fun entryTakesFirstTagAlphabetically() {
        assertEquals(tagColour(4), entryColour(listOf(tag("office", 1), tag("cricket", 4))))
    }

    @Test
    fun untaggedIsPink() {
        assertEquals(Untagged, entryColour(emptyList()))
        assertEquals(Color(0xFFF9CAD5), Untagged.fill)
    }

    @Test
    fun nameLooksUpItsSlot() {
        val slots = mapOf("gym" to 3, "office" to 0)
        assertEquals(tagColour(3), tagColour(" Gym ", slots))
    }

    @Test
    fun unsavedTagPreviewsTheSlotItWillGet() {
        assertEquals(tagColour(1), tagColour("new", mapOf("gym" to 0, "office" to 2)))
    }

    @Test
    fun tealHasWhiteText() {
        val teal = TagPalette.first { it.fill == Color(0xFF2E8A78) }
        assertEquals(Color.White, teal.onFill)
    }
}
