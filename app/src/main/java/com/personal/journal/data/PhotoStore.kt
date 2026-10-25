package com.personal.journal.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class StoredPhoto(val photoFile: String, val thumbFile: String, val exifTakenAt: String?)

class UnreadablePhotoException : IOException("This photo couldn't be read")

/**
 * App-private photo files. Photos are re-encoded (upright, resized, no EXIF/GPS) and written
 * atomically via a .tmp file + rename so a crash never leaves a half-written photo.
 */
class PhotoStore(private val filesDir: File) {
    private val photos = File(filesDir, PHOTOS).apply { mkdirs() }
    private val thumbs = File(filesDir, THUMBS).apply { mkdirs() }

    fun file(relative: String): File = File(filesDir, relative)

    fun freeBytes(): Long = filesDir.usableSpace

    suspend fun save(id: String, source: InputStream): StoredPhoto = withContext(Dispatchers.IO) {
        val src = File(photos, "$id.src.tmp")
        try {
            src.outputStream().use { source.copyTo(it) }
            val exif = runCatching { ExifInterface(src) }.getOrNull()
            val rotation = exif?.rotationDegrees ?: 0
            val exifDate = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif?.getAttribute(ExifInterface.TAG_DATETIME)

            val full = decodeUpright(src, MAX_EDGE, rotation)
            val thumb = scaleToFit(full, THUMB_EDGE)
            writeAtomically(full, File(photos, "$id.jpg"))
            writeAtomically(thumb, File(thumbs, "$id.jpg"))
            if (thumb !== full) thumb.recycle()
            full.recycle()
            StoredPhoto("$PHOTOS/$id.jpg", "$THUMBS/$id.jpg", exifDate)
        } finally {
            src.delete()
        }
    }

    fun delete(id: String) {
        File(photos, "$id.jpg").delete()
        File(thumbs, "$id.jpg").delete()
    }

    /** Removes temp files and photos whose id isn't in [knownIds]. Returns the number of files removed. */
    fun cleanup(knownIds: Set<String>): Int {
        var removed = 0
        for (dir in listOf(photos, thumbs)) {
            dir.listFiles()?.forEach { f ->
                val orphan = f.name.endsWith(".tmp") || f.nameWithoutExtension !in knownIds
                if (orphan && f.delete()) removed++
            }
        }
        return removed
    }

    private fun decodeUpright(src: File, maxEdge: Int, rotation: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw UnreadablePhotoException()
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val decoded = BitmapFactory.decodeFile(src.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw UnreadablePhotoException()
        val rotated = if (rotation == 0) decoded else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
                .also { decoded.recycle() }
        }
        return scaleToFit(rotated, maxEdge).also { if (it !== rotated) rotated.recycle() }
    }

    private fun scaleToFit(bmp: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = max(bmp.width, bmp.height)
        if (longEdge <= maxEdge) return bmp
        val scale = maxEdge.toFloat() / longEdge
        return Bitmap.createScaledBitmap(bmp, (bmp.width * scale).roundToInt(), (bmp.height * scale).roundToInt(), true)
    }

    private fun writeAtomically(bmp: Bitmap, target: File) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        FileOutputStream(tmp).use { out ->
            if (!bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) throw IOException("Could not encode photo")
            out.fd.sync()
        }
        if (!tmp.renameTo(target)) throw IOException("Could not save photo")
    }

    companion object {
        const val PHOTOS = "photos"
        const val THUMBS = "thumbs"
        const val MAX_EDGE = 2560
        const val THUMB_EDGE = 480
        const val JPEG_QUALITY = 85
    }
}
