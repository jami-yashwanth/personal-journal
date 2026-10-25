package com.personal.journal.ui.importer

import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.personal.journal.data.db.DateSource
import com.personal.journal.testing.TestJournal
import com.personal.journal.testing.observeDayFirst
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class ImportViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var j: TestJournal

    @Before fun setUp() { j = TestJournal(tmp.root) }
    @After fun tearDown() = j.close()

    private fun withExif(date: String): File = j.jpegFile().also {
        ExifInterface(it).apply { setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, date); saveAttributes() }
    }

    private fun vm() = ImportViewModel(
        j.repo,
        open = { uri -> uri.path?.let { File(it) }?.takeIf { it.exists() }?.inputStream() },
        zone = { j.zone },
    )

    @Test
    fun exifDateUsed() = runTest {
        val vm = vm()
        vm.load(listOf(Uri.fromFile(withExif("2026:10:23 19:05:00"))))
        val item = vm.items.value.single()
        assertTrue(item.hasExifDate)
        assertEquals(j.millis("2026-10-23T19:05:00+05:30"), item.takenAt)
        val saved = vm.saveAll()
        assertEquals(1, saved.saved)
        assertEquals("2026-10-23", j.repo.observeDayFirst("2026-10-23").single().entry.localDate)
    }

    @Test
    fun missingExifFallsBackToToday() = runTest {
        val vm = vm()
        vm.load(listOf(Uri.fromFile(j.jpegFile())))
        val item = vm.items.value.single()
        assertFalse(item.hasExifDate)
        assertEquals(j.now.toEpochMilli(), item.takenAt)
        vm.saveAll()
        assertEquals(DateSource.FALLBACK_TODAY, j.repo.observeDayFirst("2026-10-25").single().entry.dateSource)
    }

    @Test
    fun sharedTagsAppliedToAll() = runTest {
        val vm = vm()
        vm.load(listOf(Uri.fromFile(j.jpegFile()), Uri.fromFile(j.jpegFile())))
        vm.tags.toggle("cricket")
        vm.setCaption(0, "Match day")
        vm.saveAll()
        val day = j.repo.observeDayFirst("2026-10-25")
        assertEquals(2, day.size)
        assertTrue(day.all { e -> e.tags.map { it.name } == listOf("cricket") })
        assertNotNull(day.firstOrNull { it.entry.text == "Match day" })
    }

    @Test
    fun partialFailureKeepsOthers() = runTest {
        val vm = vm()
        vm.load(listOf(Uri.fromFile(j.jpegFile()), Uri.fromFile(File(tmp.root, "missing.jpg"))))
        assertEquals("This photo couldn't be read", vm.items.value[1].error)
        val result = vm.saveAll()
        assertEquals(1, result.saved)
        assertEquals(listOf(1), result.failed)
    }

    @Test
    fun manualDateMarksManual() = runTest {
        val vm = vm()
        vm.load(listOf(Uri.fromFile(j.jpegFile())))
        vm.setDate(0, j.millis("2026-10-10T10:00:00+05:30"))
        vm.saveAll()
        assertEquals(DateSource.MANUAL, j.repo.observeDayFirst("2026-10-10").single().entry.dateSource)
    }
}
