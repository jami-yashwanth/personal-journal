package com.personal.journal.data

const val MAX_TAG_LENGTH = 24

private val spaces = Regex("\\s+")

/** Lowercase, trimmed, inner whitespace collapsed; null when empty or longer than [MAX_TAG_LENGTH]. */
fun normalizeTag(raw: String): String? {
    val name = raw.trim().replace(spaces, " ").lowercase()
    return if (name.isEmpty() || name.length > MAX_TAG_LENGTH) null else name
}

/** How many tag colours there are; TagPalette must have exactly this many. */
const val TAG_COLOUR_SLOTS = 6

/** The lowest slot no tag has yet; once all are taken, the least-used one (lowest on a tie). */
fun nextColourSlot(used: Collection<Int>): Int {
    val counts = IntArray(TAG_COLOUR_SLOTS)
    used.forEach { counts[it.mod(TAG_COLOUR_SLOTS)]++ }
    return counts.indices.minBy { counts[it] }
}
