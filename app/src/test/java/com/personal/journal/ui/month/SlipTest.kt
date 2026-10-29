package com.personal.journal.ui.month

import org.junit.Assert.assertEquals
import org.junit.Test

class SlipTest {
    @Test
    fun aloneCapsAtFour() = assertEquals(4, slipMaxLines(1000f, 20f, 12f, alone = true))

    @Test
    fun sharedCapsAtThree() = assertEquals(3, slipMaxLines(1000f, 20f, 12f, alone = false))

    @Test
    fun shortTileStillShowsOneLine() = assertEquals(1, slipMaxLines(25f, 20f, 12f, alone = false))

    @Test
    fun fitsWhatRoomAllows() = assertEquals(2, slipMaxLines(60f, 20f, 12f, alone = false))
}
