package com.personal.journal.data

const val MAX_TAG_LENGTH = 24

private val spaces = Regex("\\s+")

/** Lowercase, trimmed, inner whitespace collapsed; null when empty or longer than [MAX_TAG_LENGTH]. */
fun normalizeTag(raw: String): String? {
    val name = raw.trim().replace(spaces, " ").lowercase()
    return if (name.isEmpty() || name.length > MAX_TAG_LENGTH) null else name
}
