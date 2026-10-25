package com.personal.journal.ui.month

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.journal.bento.PlacedTile

/** Places one child per [tiles] entry (same order) on a 3-column grid of [cellHeight] rows. */
@Composable
fun BentoGrid(
    tiles: List<PlacedTile>,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    cellHeight: Dp = 110.dp,
    gap: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val cellH = cellHeight.roundToPx()
        val cellW = (constraints.maxWidth - gapPx * (columns - 1)) / columns
        val rows = tiles.maxOfOrNull { it.row + it.size.rows } ?: 0
        val placeables = measurables.zip(tiles).map { (m, t) ->
            val w = cellW * t.size.cols + gapPx * (t.size.cols - 1)
            val h = cellH * t.size.rows + gapPx * (t.size.rows - 1)
            m.measure(Constraints.fixed(w, h)) to t
        }
        val height = if (rows == 0) 0 else rows * cellH + (rows - 1) * gapPx
        layout(constraints.maxWidth, height) {
            placeables.forEach { (p, t) -> p.place(t.col * (cellW + gapPx), t.row * (cellH + gapPx)) }
        }
    }
}
