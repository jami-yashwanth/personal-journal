package com.personal.journal.bento

import java.util.Random
import kotlin.math.ceil
import kotlin.math.sqrt

/** Where one item lands inside a tile: position and size as fractions of the tile, tilt in degrees. */
data class Spot(val x: Float, val y: Float, val width: Float, val height: Float, val rotation: Float)

const val MAX_TILT = 7f

/**
 * Lays [count] items out like prints dropped on a table: a loose grid sized to the tile's [aspect] (width / height),
 * each item nudged and tilted a little so neighbours overlap, but kept inside the tile so nothing is cut off. The same [seed] always
 * gives the same layout, so a day looks the same every time the month is opened.
 */
fun scatter(count: Int, aspect: Float, seed: Int): List<Spot> {
    if (count <= 0) return emptyList()
    val random = Random(seed.toLong())
    fun spread(range: Float) = (random.nextFloat() - 0.5f) * range
    if (count == 1) {
        val w = 0.78f
        val h = 0.72f
        return listOf(Spot((1 - w) / 2 + spread(0.08f), (1 - h) / 2 + spread(0.08f), w, h, spread(MAX_TILT)))
    }
    val cols = ceil(sqrt(count * aspect)).toInt().coerceIn(1, count)
    val rows = ceil(count / cols.toFloat()).toInt()
    val cellW = 1f / cols
    val cellH = 1f / rows
    val w = cellW * 0.95f
    val h = cellH * 0.95f
    return List(count) { i ->
        val row = i / cols
        // A short last row sits in the middle rather than hugging the left edge.
        val inRow = if (row == rows - 1) count - row * cols else cols
        val col = i % cols
        val cx = (col + 0.5f) * cellW + (cols - inRow) * cellW / 2 + spread(0.35f * cellW)
        val cy = (row + 0.5f) * cellH + spread(0.35f * cellH)
        Spot(
            x = cx.coerceIn(w / 2, 1 - w / 2) - w / 2,
            y = cy.coerceIn(h / 2, 1 - h / 2) - h / 2,
            width = w,
            height = h,
            rotation = spread(2 * MAX_TILT),
        )
    }
}
