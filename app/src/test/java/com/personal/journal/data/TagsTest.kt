package com.personal.journal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagsTest {
    @Test
    fun normalisation() {
        assertEquals("gym", normalizeTag("  Gym "))
        assertEquals("gym", normalizeTag("GYM"))
        assertEquals("badminton", normalizeTag("badminton"))
        assertEquals("a".repeat(24), normalizeTag("a".repeat(24)))
        assertNull(normalizeTag("a".repeat(30)))
        assertNull(normalizeTag(""))
        assertNull(normalizeTag("   "))
    }

    @Test
    fun innerWhitespaceCollapses() {
        assertEquals("table tennis", normalizeTag("Table   Tennis"))
    }
}
