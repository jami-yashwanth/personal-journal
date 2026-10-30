package com.personal.journal.ui.month

import kotlin.math.floor

/** Lines a slip may show: as many as its tile has room for, capped at 4 when it is the tile's only item and 3 otherwise. */
fun slipMaxLines(availableHeightPx: Float, lineHeightPx: Float, verticalPaddingPx: Float, alone: Boolean): Int {
    val fits = floor((availableHeightPx - verticalPaddingPx) / lineHeightPx).toInt()
    return minOf(if (alone) 4 else 3, maxOf(1, fits))
}
