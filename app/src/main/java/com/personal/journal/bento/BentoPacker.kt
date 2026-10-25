package com.personal.journal.bento

data class DaySummary(
    val localDate: String,
    val photoCount: Int,
    val textCount: Int,
    val firstTextLength: Int,
    val isToday: Boolean,
)

enum class TileSize(val cols: Int, val rows: Int) { SMALL(1, 1), WIDE(2, 1), LARGE(2, 2) }

data class PlacedTile(val localDate: String, val size: TileSize, val col: Int, val row: Int)

private const val LONG_TEXT = 60

fun sizeFor(day: DaySummary): TileSize = when {
    day.isToday -> TileSize.LARGE
    day.photoCount >= 4 -> TileSize.LARGE
    day.photoCount >= 2 -> TileSize.WIDE
    day.photoCount == 1 -> TileSize.SMALL
    day.firstTextLength > LONG_TEXT -> TileSize.WIDE
    else -> TileSize.SMALL
}

/**
 * Greedy first-fit packing. [days] must be sorted newest first. Each tile goes in the first
 * row-major slot where it fits, so a SMALL tile can drop into a hole left by an earlier wide tile;
 * larger tiles therefore keep date order while small ones may move up to fill gaps.
 */
fun pack(days: List<DaySummary>, columns: Int = 3): List<PlacedTile> {
    val grid = mutableListOf<BooleanArray>()
    fun free(c: Int, r: Int) = r >= grid.size || !grid[r][c]
    fun fits(size: TileSize, c: Int, r: Int): Boolean {
        if (c + size.cols > columns) return false
        for (dc in 0 until size.cols) for (dr in 0 until size.rows) if (!free(c + dc, r + dr)) return false
        return true
    }
    fun occupy(size: TileSize, c: Int, r: Int) {
        while (grid.size < r + size.rows) grid.add(BooleanArray(columns))
        for (dc in 0 until size.cols) for (dr in 0 until size.rows) grid[r + dr][c + dc] = true
    }

    var cursor = 0 // first row that may still contain a free cell
    return days.map { day ->
        val size = sizeFor(day)
        var row = cursor
        var placed: PlacedTile? = null
        while (placed == null) {
            for (col in 0 until columns) {
                if (fits(size, col, row)) {
                    occupy(size, col, row)
                    placed = PlacedTile(day.localDate, size, col, row)
                    break
                }
            }
            row++
        }
        while (cursor < grid.size && grid[cursor].all { it }) cursor++
        placed
    }
}
