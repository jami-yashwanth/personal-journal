package com.personal.journal.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.personal.journal.data.db.TagEntity
import com.personal.journal.data.nextColourSlot
import com.personal.journal.data.normalizeTag

/** A tag's fill and the colour of anything drawn on top of it. */
data class TagColour(val fill: Color, val onFill: Color)

private val OnColour = Color(0xFF1E1B19)

/** In spec order. A tag's stored colourSlot indexes this list, so reordering recolours existing tags. */
val TagPalette: List<TagColour> = listOf(
    TagColour(Color(0xFF2E8A78), Color.White), // teal
    TagColour(Color(0xFFF7954B), OnColour), // orange
    TagColour(Color(0xFFB8B3F6), OnColour), // lavender
    TagColour(Color(0xFFF5B840), OnColour), // mustard
    TagColour(Color(0xFF8CC8F0), OnColour), // sky
    TagColour(Color(0xFFA8D5A2), OnColour), // sage
)

/** Entries with no tags. */
val Untagged = TagColour(Color(0xFFF9CAD5), OnColour)

fun tagColour(slot: Int): TagColour = TagPalette[slot.mod(TagPalette.size)]

/** An entry takes the colour of its alphabetically first tag. */
fun entryColour(tags: List<TagEntity>): TagColour =
    tags.minByOrNull { it.name }?.let { tagColour(it.colourSlot) } ?: Untagged

/** Every saved tag's colour slot by name, provided at the root from the tags table. */
val LocalTagSlots = staticCompositionLocalOf<Map<String, Int>> { emptyMap() }

/** A tag's colour by name; a tag not saved yet previews the slot it would get. */
fun tagColour(name: String, slots: Map<String, Int>): TagColour =
    tagColour(slots[normalizeTag(name) ?: name] ?: nextColourSlot(slots.values))

@Composable
fun tagColour(name: String): TagColour = tagColour(name, LocalTagSlots.current)
