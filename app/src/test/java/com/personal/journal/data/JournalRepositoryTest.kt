package com.personal.journal.data

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.journal.data.db.DateSource
import com.personal.journal.data.db.JournalDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JournalRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: JournalDatabase
    private lateinit var photos: PhotoStore
    private var zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val now = OffsetDateTime.parse("2026-10-25T23:59:00+05:30").toInstant()
    private var free = 10L * 1024 * 1024 * 1024
    private var changes = 0
    private lateinit var repo: JournalRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), JournalDatabase::class.java)
            .allowMainThreadQueries().build()
        photos = PhotoStore(tmp.newFolder("files"))
        repo = JournalRepository(
            db = db, photos = photos, clock = Clock.fixed(now, ZoneOffset.UTC), zone = { zone },
            onChanged = { changes++ }, freeBytes = { free },
        )
    }

    @After
    fun tearDown() = db.close()

    private fun jpeg(exifDate: String? = null): ByteArrayInputStream {
        val f = tmp.newFile()
        val bmp = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (exifDate != null) ExifInterface(f).apply { setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate); saveAttributes() }
        return ByteArrayInputStream(f.readBytes())
    }

    @Test
    fun photoWithoutExifFallsBackToToday() = runTest {
        val id = repo.savePhoto(jpeg(), caption = null, tags = emptyList(), takenAt = null, dateSource = DateSource.EXIF)
        val e = repo.get(id)!!.entry
        assertEquals(DateSource.FALLBACK_TODAY, e.dateSource)
        assertEquals(now.toEpochMilli(), e.takenAt)
        assertEquals("2026-10-25", e.localDate)
    }

    @Test
    fun photoWithExifUsesItsDate() = runTest {
        val id = repo.savePhoto(jpeg("2026:10:23 19:05:00"), null, emptyList(), takenAt = null, dateSource = DateSource.EXIF)
        val e = repo.get(id)!!.entry
        assertEquals(DateSource.EXIF, e.dateSource)
        assertEquals("2026-10-23", e.localDate)
    }

    @Test
    fun cameraPhotoUsesGivenTime() = runTest {
        val id = repo.savePhoto(jpeg("2020:01:01 00:00:00"), "First coffee", listOf("Office"), now.toEpochMilli(), DateSource.CAMERA)
        val e = repo.get(id)!!
        assertEquals(DateSource.CAMERA, e.entry.dateSource)
        assertEquals("2026-10-25", e.entry.localDate)
        assertEquals("First coffee", e.entry.text)
        assertEquals(listOf("office"), e.tags.map { it.name })
        assertTrue(photos.file(e.entry.photoFile!!).exists())
    }

    @Test
    fun textSavesWithTags() = runTest {
        val id = repo.saveText("Leg day", listOf("Gym", "gym ", "", "x".repeat(30)), now.toEpochMilli())
        val e = repo.get(id)!!
        assertEquals(listOf("gym"), e.tags.map { it.name })
        assertEquals(1, db.tagDao().findByName("gym")!!.useCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankTextRejected() = runTest {
        repo.saveText("   ", emptyList(), now.toEpochMilli())
    }

    @Test
    fun zoneChangeKeepsDay() = runTest {
        val id = repo.saveText("late", emptyList(), now.toEpochMilli())
        zone = ZoneOffset.UTC
        assertEquals(listOf(id), repo.observeDay("2026-10-25").first().map { it.entry.id })
    }

    @Test
    fun manualDateRecomputesDay() = runTest {
        val id = repo.saveText("x", emptyList(), now.toEpochMilli())
        val newTime = OffsetDateTime.parse("2026-10-20T10:00:00+05:30").toInstant().toEpochMilli()
        repo.update(id, text = null, tags = null, takenAt = newTime)
        val e = repo.get(id)!!.entry
        assertEquals("2026-10-20", e.localDate)
        assertEquals(DateSource.MANUAL, e.dateSource)
    }

    @Test
    fun updateTagsOnlyCountsNewOnes() = runTest {
        val id = repo.saveText("x", listOf("gym"), now.toEpochMilli())
        repo.update(id, text = "y", tags = listOf("gym", "office"), takenAt = null)
        assertEquals(1, db.tagDao().findByName("gym")!!.useCount)
        assertEquals(1, db.tagDao().findByName("office")!!.useCount)
        assertEquals(setOf("gym", "office"), repo.get(id)!!.tags.map { it.name }.toSet())
    }

    @Test
    fun deleteRemovesFilesKeepsTombstone() = runTest {
        val id = repo.savePhoto(jpeg(), null, emptyList(), now.toEpochMilli(), DateSource.CAMERA)
        val photo = photos.file(repo.get(id)!!.entry.photoFile!!)
        db.entryDao().markBackedUp(id, Long.MAX_VALUE)
        repo.delete(id)
        assertFalse(photo.exists())
        val row = db.entryDao().getById(id)
        assertNotNull(row!!.entry.deletedAt)
        assertTrue(repo.observeDay("2026-10-25").first().isEmpty())
    }

    @Test
    fun deleteAlwaysLeavesContentFreeTombstone() = runTest {
        // Even never-backed-up entries keep a tombstone: an upload may already be in flight.
        val id = repo.saveText("private words", listOf("gym"), now.toEpochMilli())
        repo.delete(id)
        val row = db.entryDao().getById(id)!!
        assertNotNull(row.entry.deletedAt)
        assertNull(row.entry.text)
        assertTrue(row.tags.isEmpty())
        assertNull(repo.get(id))
    }

    @Test
    fun startupCleanupSweepsRawCaptures() = runTest {
        val cache = tmp.newFolder("cache")
        File(cache, "capture-123.jpg").writeText("raw with gps")
        File(cache, "other.txt").writeText("keep")
        assertEquals(1, repo.startupCleanup(cache))
        assertFalse(File(cache, "capture-123.jpg").exists())
        assertTrue(File(cache, "other.txt").exists())
    }

    @Test
    fun onChangedFiresOnWrites() = runTest {
        val id = repo.saveText("x", emptyList(), now.toEpochMilli())
        repo.update(id, "y", null, null)
        repo.delete(id)
        assertEquals(3, changes)
    }

    @Test(expected = InsufficientStorageException::class)
    fun lowStorageRejects() = runTest {
        free = 10L * 1024 * 1024
        repo.savePhoto(jpeg(), null, emptyList(), now.toEpochMilli(), DateSource.CAMERA)
    }

    @Test
    fun startupCleanupRemovesOrphans() = runTest {
        File(photos.file("photos"), "stray.jpg").writeText("x")
        assertEquals(1, repo.startupCleanup())
    }

    @Test
    fun todayIsInDeviceZone() {
        assertEquals("2026-10-25", repo.today())
        zone = ZoneOffset.UTC
        assertEquals("2026-10-25", repo.today())
        assertEquals(Instant.parse("2026-10-25T18:29:00Z").toEpochMilli(), now.toEpochMilli())
    }
}
