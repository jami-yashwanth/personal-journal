package com.personal.journal.ui.entry

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TINT_SIZE = 32
private const val MAX_LONG_SIDE = 64

/**
 * A pale (light) or deep (dark) wash of the photo's average hue, for the entry
 * page background. Near-black and near-white pixels are ignored; null when
 * nothing usable is left.
 */
fun photoTint(argbPixels: IntArray, dark: Boolean): Color? {
    var sumX = 0.0
    var sumY = 0.0
    var sumS = 0.0
    var n = 0
    for (p in argbPixels) {
        val r = ((p shr 16) and 0xFF) / 255f
        val g = ((p shr 8) and 0xFF) / 255f
        val b = (p and 0xFF) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        if (l < 0.08f || l > 0.95f) continue
        val d = mx - mn
        val s = if (d < 1e-6f) 0f else d / (1f - abs(2f * l - 1f))
        val h = when {
            d < 1e-6f -> 0f
            mx == r -> 60f * (((g - b) / d + 6f) % 6f)
            mx == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }
        val rad = Math.toRadians(h.toDouble())
        // weight by saturation so grey pixels (hue 0 by convention) don't pull the mean
        sumX += s * cos(rad)
        sumY += s * sin(rad)
        sumS += s
        n++
    }
    if (n == 0) return null
    var hue = Math.toDegrees(atan2(sumY, sumX)).toFloat()
    if (hue < 0f) hue += 360f
    val sat = min((sumS / n).toFloat(), if (dark) 0.30f else 0.45f)
    return Color.hsl(hue, sat, if (dark) 0.14f else 0.92f)
}

/** Decodes thumbnails once per entry and theme; failures are not cached. */
class TintCache {
    private val cache = ConcurrentHashMap<String, Color>()

    suspend fun tint(entryId: String, thumb: File?, dark: Boolean): Color? {
        if (thumb == null) return null
        return withContext(Dispatchers.IO) {
            // lastModified so a retaken photo does not keep a stale tint
            val key = "$entryId-$dark-${thumb.lastModified()}"
            cache[key]?.let { return@withContext it }
            val color = decode(thumb, dark) ?: return@withContext null
            cache[key] = color
            color
        }
    }

    private fun decode(file: File, dark: Boolean): Color? = try {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        var side = max(bounds.outWidth, bounds.outHeight)
        while (side / 2 >= MAX_LONG_SIDE) { sample *= 2; side /= 2 }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeFile(file.path, opts)
        if (bmp == null) null else {
            val small = Bitmap.createScaledBitmap(bmp, TINT_SIZE, TINT_SIZE, true)
            val px = IntArray(TINT_SIZE * TINT_SIZE)
            small.getPixels(px, 0, TINT_SIZE, 0, 0, TINT_SIZE, TINT_SIZE)
            photoTint(px, dark)
        }
    } catch (e: Exception) {
        null
    }
}
