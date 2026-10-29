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

    @Test
    fun nextSlotIsLowestFree() {
        assertEquals(0, nextColourSlot(emptyList()))
        assertEquals(1, nextColourSlot(listOf(0, 2)))
        assertEquals(5, nextColourSlot(listOf(0, 1, 2, 3, 4)))
    }

    @Test
    fun nextSlotWrapsToLeastUsed() {
        assertEquals(0, nextColourSlot(listOf(0, 1, 2, 3, 4, 5)))
        assertEquals(2, nextColourSlot(listOf(0, 1, 2, 3, 4, 5, 0, 1)))
        assertEquals(3, nextColourSlot(listOf(0, 0, 1, 1, 2, 2, 3, 4, 4, 5, 5)))
    }
}
