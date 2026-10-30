package com.personal.journal.ui.motion

import coil3.memory.MemoryCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageKeysTest {
    private val keys = ImageKeys()

    @Test fun putThenGet() {
        keys.put("a", MemoryCache.Key("k"))
        assertEquals(MemoryCache.Key("k"), keys.get("a"))
    }

    @Test fun missingIsNull() {
        assertNull(keys.get("nope"))
    }

    @Test fun evictsOldestPast200() {
        repeat(201) { keys.put("e$it", MemoryCache.Key("k$it")) }
        assertNull(keys.get("e0"))
        assertEquals(MemoryCache.Key("k200"), keys.get("e200"))
    }
}
