package com.personal.journal.ui.entry

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@RunWith(RobolectricTestRunner::class)
class PhotoTintTest {
    private val red = IntArray(64) { 0xFFFF0000.toInt() }
    private val blue = IntArray(64) { 0xFF0000FF.toInt() }

    // [hue 0..360, saturation, lightness]
    private fun hsl(c: Color): FloatArray {
        val r = c.red; val g = c.green; val b = c.blue
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d < 1e-6f) return floatArrayOf(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val h = when (mx) {
            r -> 60f * (((g - b) / d + 6f) % 6f)
            g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }
        return floatArrayOf(h, s, l)
    }

    private fun hueArgb(h: Float): Int = Color.hsl(h, 1f, 0.5f).toArgb()

    @Test
    fun lightnessIsFixed() {
        assertEquals(0.92f, hsl(photoTint(red, false)!!)[2], 0.01f)
        assertEquals(0.14f, hsl(photoTint(red, true)!!)[2], 0.01f)
    }

    @Test
    fun saturationClamped() {
        // output is 8-bit sRGB, so allow rounding slack
        assertTrue(hsl(photoTint(red, false)!!)[1] <= 0.45f + 0.02f)
        assertTrue(hsl(photoTint(red, true)!!)[1] <= 0.30f + 0.02f)
    }

    private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)

    @Test
    fun hueFollowsPhoto() {
        assertEquals(240f, hsl(photoTint(blue, false)!!)[0], 2f)
    }

    @Test
    fun neutralPixelsDontPullHue() {
        val px = IntArray(64) { if (it % 2 == 0) 0xFF808080.toInt() else blue[0] }
        assertEquals(240f, hsl(photoTint(px, false)!!)[0], 3f)
    }

    @Test
    fun circularHue() {
        val px = IntArray(64) { if (it % 2 == 0) hueArgb(350f) else hueArgb(10f) }
        val h = hsl(photoTint(px, false)!!)[0]
        val dist = min(h, 360f - h)
        assertTrue("hue $h", dist <= 3f)
    }

    private fun assertTrue(msg: String, b: Boolean) = org.junit.Assert.assertTrue(msg, b)

    @Test
    fun nullWhenNoUsablePixels() {
        assertNull(photoTint(IntArray(16) { 0xFF000000.toInt() }, false))
        assertNull(photoTint(IntArray(16) { 0xFFFFFFFF.toInt() }, false))
    }

    @Test
    fun greyscaleIsNeutral() {
        val t = photoTint(IntArray(16) { 0xFF808080.toInt() }, false)
        assertNotNull(t)
        assertEquals(0f, hsl(t!!)[1], 0.01f)
    }

    @Test
    fun darkAndLightTintsDiffer() {
        assertNotEquals(photoTint(red, true), photoTint(red, false))
    }

    @Test
    fun missingThumbnailFallsBack() = runTest {
        assertNull(TintCache().tint("x", File("/nope.jpg"), false))
        assertNull(TintCache().tint("x", null, false))
    }
}
