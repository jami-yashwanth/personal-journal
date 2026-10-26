package com.personal.journal.bento

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ScatterTest {
    @Test
    fun sameSeedSameLayout() {
        assertEquals(scatter(5, aspect = 2f, seed = 42), scatter(5, aspect = 2f, seed = 42))
    }

    @Test
    fun differentDaysLookDifferent() {
        assertTrue(scatter(3, aspect = 1f, seed = 1) != scatter(3, aspect = 1f, seed = 2))
    }

    @Test
    fun oneSpotPerItem() {
        for (n in 0..6) assertEquals(n, scatter(n, aspect = 1f, seed = n).size)
    }

    @Test
    fun tiltsStaySubtle() {
        for (seed in 0..50) for (n in 1..6) {
            scatter(n, aspect = 2f, seed = seed).forEach { assertTrue("rotation ${it.rotation}", abs(it.rotation) <= MAX_TILT) }
        }
    }

    @Test
    fun itemsStayInsideTheTile() {
        for (seed in 0..50) for (n in 1..6) for (aspect in listOf(1f, 2f, 1.04f)) {
            scatter(n, aspect, seed).forEach {
                // Nothing may be cut off by the tile's edge (before tilt).
                assertTrue("left ${it.x}", it.x >= -0.001f)
                assertTrue("right ${it.x + it.width}", it.x + it.width <= 1.001f)
                assertTrue("top ${it.y}", it.y >= -0.001f)
                assertTrue("bottom ${it.y + it.height}", it.y + it.height <= 1.001f)
            }
        }
    }

    @Test
    fun aLoneItemFillsMostOfTheTile() {
        val only = scatter(1, aspect = 1f, seed = 7).single()
        assertTrue(only.width > 0.7f && only.height > 0.6f)
    }
}
