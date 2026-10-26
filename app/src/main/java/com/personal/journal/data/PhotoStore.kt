package com.personal.journal.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
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
    /** Serialises "write photo + insert row" against cleanup so a fresh photo is never swept as an orphan. */
    val writeLock = Mutex()

    private val photos = File(filesDir, PHOTOS).apply { mkdirs() }
    private val thumbs = File(filesDir, THUMBS).apply { mkdirs() }

    fun file(relative: String): File = File(filesDir, relative)

    fun freeBytes(): Long = filesDir.usableSpace

    suspend fun save(id: String, source: InputStream): StoredPhoto = withContext(Dispatchers.IO) {
        val src = File(photos, "$id.src.tmp")
        try {
            src.outputStream().use { source.copyTo(it) }
            val exif = runCatching { ExifInterface(src) }.getOrNull()
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL
            val exifDate = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif?.getAttribute(ExifInterface.TAG_DATETIME)

            val full = decodeUpright(src, MAX_EDGE, orientation)
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

    private fun decodeUpright(src: File, maxEdge: Int, orientation: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw UnreadablePhotoException()
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val decoded = BitmapFactory.decodeFile(src.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw UnreadablePhotoException()
        val upright = uprightMatrix(orientation)
        val rotated = if (upright == null) decoded else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, upright, true).also { decoded.recycle() }
        }
        return scaleToFit(rotated, maxEdge).also { if (it !== rotated) rotated.recycle() }
    }

    /**
     * The transform that shows a photo the way its EXIF orientation says. The mirrored orientations matter: the front
     * camera tags selfies as flipped so they match the mirrored preview, and rotating them alone turns them upside down.
     */
    private fun uprightMatrix(orientation: Int): Matrix? = when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { setScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { setRotate(180f) }
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { setRotate(180f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { setRotate(90f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { setRotate(90f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply { setRotate(-90f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { setRotate(-90f) }
        else -> null
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
