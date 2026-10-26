package com.personal.journal.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var store: PhotoStore
    private lateinit var files: File

    @Before
    fun setUp() {
        files = tmp.newFolder("files")
        store = PhotoStore(files)
    }

    private fun jpeg(w: Int, h: Int, exif: (ExifInterface) -> Unit = {}): File {
        val f = tmp.newFile()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(f).apply { exif(this); saveAttributes() }
        return f
    }

    private fun size(f: File): Pair<Int, Int> {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        return o.outWidth to o.outHeight
    }

    @Test
    fun savesUpright() = runTest {
        val src = jpeg(400, 200) { it.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()) }
        val saved = src.inputStream().use { store.save("a", it) }
        val out = store.file(saved.photoFile)
        assertEquals(200 to 400, size(out))
        val orientation = ExifInterface(out).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        assertTrue(orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED)
    }

    /**
     * Every EXIF orientation, including the mirrored ones the front camera writes, must come out upright: each source
     * is laid out so that, displayed as its tag says, it is 200x100 with a red top-left quarter.
     */
    @Test
    fun savesEveryOrientationUpright() = runTest {
        // Orientation tag to where the red quarter sits in the stored (raw) pixels, and whether width/height swap.
        val cases = mapOf(
            ExifInterface.ORIENTATION_NORMAL to Triple(false, false, false),
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to Triple(true, false, false),
            ExifInterface.ORIENTATION_ROTATE_180 to Triple(true, true, false),
            ExifInterface.ORIENTATION_FLIP_VERTICAL to Triple(false, true, false),
            ExifInterface.ORIENTATION_TRANSPOSE to Triple(false, false, true),
            ExifInterface.ORIENTATION_ROTATE_90 to Triple(false, true, true),
            ExifInterface.ORIENTATION_TRANSVERSE to Triple(true, true, true),
            ExifInterface.ORIENTATION_ROTATE_270 to Triple(true, false, true),
        )
        for ((orientation, layout) in cases) {
            val (right, bottom, swapped) = layout
            val (w, h) = if (swapped) 100 to 200 else 200 to 100
            val raw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
            for (x in 0 until w / 2) for (y in 0 until h / 2) {
                raw.setPixel(if (right) w - 1 - x else x, if (bottom) h - 1 - y else y, Color.RED)
            }
            val src = tmp.newFile()
            src.outputStream().use { raw.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            ExifInterface(src).apply { setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString()); saveAttributes() }

            val saved = src.inputStream().use { store.save("o$orientation", it) }
            val out = BitmapFactory.decodeFile(store.file(saved.photoFile).path)
            assertEquals("size for orientation $orientation", 200 to 100, out.width to out.height)
            assertTrue("red top-left for orientation $orientation", Color.red(out.getPixel(25, 12)) > 180 && Color.green(out.getPixel(25, 12)) < 90)
            assertTrue("grey bottom-right for orientation $orientation", Color.green(out.getPixel(175, 87)) > 90)
        }
    }

    @Test
    fun resizesToMax2560() = runTest {
        val saved = jpeg(4000, 3000).inputStream().use { store.save("b", it) }
        assertEquals(2560 to 1920, size(store.file(saved.photoFile)))
        assertEquals(480 to 360, size(store.file(saved.thumbFile)))
    }

    @Test
    fun smallPhotoIsNotUpscaled() = runTest {
        val saved = jpeg(300, 200).inputStream().use { store.save("s", it) }
        assertEquals(300 to 200, size(store.file(saved.photoFile)))
    }

    @Test
    fun stripsGps() = runTest {
        val src = jpeg(400, 300) { it.setLatLong(12.97, 77.59) }
        val saved = src.inputStream().use { store.save("c", it) }
        assertNull(ExifInterface(store.file(saved.photoFile)).getAttribute(ExifInterface.TAG_GPS_LATITUDE))
    }

    @Test
    fun returnsExifDate() = runTest {
        val src = jpeg(400, 300) { it.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:23 19:05:00") }
        val saved = src.inputStream().use { store.save("d", it) }
        assertEquals("2026:10:23 19:05:00", saved.exifTakenAt)
    }

    @Test
    fun pathsAreRelative() = runTest {
        val saved = jpeg(10, 10).inputStream().use { store.save("e", it) }
        assertEquals("photos/e.jpg", saved.photoFile)
        assertEquals("thumbs/e.jpg", saved.thumbFile)
    }

    @Test
    fun cleanupRemovesOrphansAndTmp() = runTest {
        jpeg(10, 10).inputStream().use { store.save("keep", it) }
        jpeg(10, 10).inputStream().use { store.save("orphan", it) }
        File(files, "photos/half.jpg.tmp").writeText("x")
        val removed = store.cleanup(setOf("keep"))
        assertEquals(3, removed)
        assertTrue(store.file("photos/keep.jpg").exists())
        assertFalse(store.file("photos/orphan.jpg").exists())
        assertFalse(File(files, "photos/half.jpg.tmp").exists())
    }

    @Test
    fun deleteRemovesBoth() = runTest {
        val saved = jpeg(10, 10).inputStream().use { store.save("x", it) }
        store.delete("x")
        assertFalse(store.file(saved.photoFile).exists())
        assertFalse(store.file(saved.thumbFile).exists())
    }
}
