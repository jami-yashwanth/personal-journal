package com.personal.journal.ui.theme

import androidx.compose.ui.graphics.Color
import com.personal.journal.data.normalizeTag

/** A tag's fill and the colour of anything drawn on top of it. */
data class TagColour(val fill: Color, val onFill: Color)

private val OnColour = Color(0xFF1E1B19)

/** In spec order. Index is picked by name hash, so reordering recolours existing tags. */
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

/** Stable per name: String.hashCode is defined by the JVM, so it survives restarts and devices. */
fun tagColour(name: String): TagColour =
    TagPalette[(normalizeTag(name) ?: name).hashCode().mod(TagPalette.size)]

/** An entry takes the colour of its alphabetically first tag. */
fun entryColour(tagNames: List<String>): TagColour =
    tagNames.minOrNull()?.let(::tagColour) ?: Untagged
