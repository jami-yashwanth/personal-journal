package com.personal.journal.ui.day

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoAspectTest {
    @Test
    fun keepsTheShotsOwnShape() {
        assertEquals(4f / 3f, photoAspect(480, 360)!!, 0.001f)
        assertEquals(3f / 4f, photoAspect(360, 480)!!, 0.001f)
    }

    @Test
    fun panoramasAndTallShotsAreHeldIn() {
        assertEquals(5f / 3f, photoAspect(2000, 400)!!, 0.001f)
        assertEquals(3f / 5f, photoAspect(300, 1200)!!, 0.001f)
    }

    @Test
    fun unreadableSizeHasNoShape() {
        assertNull(photoAspect(0, 0))
        assertNull(photoAspect(-1, 300))
    }
}
