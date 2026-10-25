package com.personal.journal.bento

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BentoPackerTest {
    private fun day(d: Int, photos: Int = 0, texts: Int = 0, textLen: Int = 10, today: Boolean = false) =
        DaySummary("2026-10-%02d".format(d), photos, texts, textLen, today)

    @Test
    fun sizeRules() {
        assertEquals(TileSize.LARGE, sizeFor(day(1, photos = 0, texts = 1, today = true)))
        assertEquals(TileSize.SMALL, sizeFor(day(1, photos = 1)))
        assertEquals(TileSize.WIDE, sizeFor(day(1, photos = 2)))
        assertEquals(TileSize.WIDE, sizeFor(day(1, photos = 3, texts = 2)))
        assertEquals(TileSize.LARGE, sizeFor(day(1, photos = 4)))
        assertEquals(TileSize.SMALL, sizeFor(day(1, texts = 1, textLen = 60)))
        assertEquals(TileSize.WIDE, sizeFor(day(1, texts = 1, textLen = 61)))
    }

    @Test
    fun emptyInputReturnsEmpty() = assertTrue(pack(emptyList()).isEmpty())

    @Test
    fun todayAtTopLeft() {
        val tiles = pack(listOf(day(25, photos = 1, today = true), day(24, texts = 1), day(23, photos = 1)))
        assertEquals(PlacedTile("2026-10-25", TileSize.LARGE, 0, 0), tiles.first())
    }

    @Test
    fun gapFilledBySmall() {
        // LARGE at (0,0) leaves column 2 of rows 0-1 empty; WIDE tiles can't fit there, the two SMALL days fill it.
        val tiles = pack(
            listOf(day(25, today = true, photos = 1), day(24, photos = 2), day(23, photos = 2), day(22, photos = 1), day(21, texts = 1)),
        )
        val occupied = occupancy(tiles, rows = 2)
        assertTrue("rows 0-1 fully used: $tiles", occupied.all { row -> row.all { it } })
    }

    @Test
    fun noOverlapAndWithinColumns() {
        val rnd = Random(7)
        for (n in listOf(1, 7, 14, 31)) {
            val days = (n downTo 1).map { d ->
                day(d, photos = rnd.nextInt(0, 6), texts = rnd.nextInt(0, 3), textLen = rnd.nextInt(0, 120), today = d == n)
            }.map { if (it.photoCount == 0 && it.textCount == 0) it.copy(textCount = 1) else it }
            val tiles = pack(days)
            assertEquals(n, tiles.size)
            assertEquals(days.map { it.localDate }.toSet(), tiles.map { it.localDate }.toSet())
            val cells = mutableSetOf<Pair<Int, Int>>()
            for (t in tiles) {
                assertTrue(t.col >= 0 && t.col + t.size.cols <= 3)
                for (c in t.col until t.col + t.size.cols) for (r in t.row until t.row + t.size.rows) {
                    assertTrue("overlap at $c,$r", cells.add(c to r))
                }
            }
        }
    }

    @Test
    fun largeTilesKeepDateOrder() {
        val tiles = pack(listOf(day(25, today = true, photos = 1), day(20, photos = 5), day(10, photos = 4)))
        val large = tiles.filter { it.size == TileSize.LARGE }.sortedWith(compareBy({ it.row }, { it.col }))
        assertEquals(listOf("2026-10-25", "2026-10-20", "2026-10-10"), large.map { it.localDate })
    }

    private fun occupancy(tiles: List<PlacedTile>, rows: Int): Array<BooleanArray> {
        val grid = Array(rows) { BooleanArray(3) }
        for (t in tiles) for (c in t.col until t.col + t.size.cols) for (r in t.row until t.row + t.size.rows) {
            if (r < rows) grid[r][c] = true
        }
        return grid
    }
}
