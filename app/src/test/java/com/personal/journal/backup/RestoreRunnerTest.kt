package com.personal.journal.backup

import com.personal.journal.data.db.DateSource
import com.personal.journal.testing.TestJournal
import com.personal.journal.testing.observeDayFirst
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
class RestoreRunnerTest {
    @get:Rule val tmp = TemporaryFolder()
    private val words = File("src/main/res/raw/bip39_english.txt").readLines().filter { it.isNotBlank() }
    private val phrases = RecoveryPhrase(words)
    private val phrase = phrases.fromEntropy(ByteArray(16) { 5 })
    private val key = deriveMasterKey(phrases.toEntropy(phrase))
    private val store = FakeObjectStore()
    private lateinit var source: TestJournal
    private lateinit var target: TestJournal

    @Before
    fun setUp() {
        source = TestJournal(tmp.newFolder("a"))
        target = TestJournal(tmp.newFolder("b"))
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    private suspend fun backupSample(): Pair<String, String> {
        val photo = source.jpegFile().inputStream().use {
            source.repo.savePhoto(it, "First coffee", listOf("office"), source.now.toEpochMilli(), DateSource.CAMERA)
        }
        val text = source.repo.saveText("Leg day", listOf("gym"), source.now.toEpochMilli())
        val gone = source.repo.saveText("delete me", emptyList(), source.now.toEpochMilli())
        BackupRunner(source.db, source.photos, store, key).runOnce()
        source.repo.delete(gone)
        BackupRunner(source.db, source.photos, store, key).runOnce()
        return photo to text
    }

    private fun runner() = RestoreRunner(target.db, target.photos, store, phrases)

    @Test
    fun wrongPhraseFailsBeforeDownloads() = runTest {
        backupSample()
        store.gets.clear()
        val wrong = phrases.fromEntropy(ByteArray(16) { 6 })
        val e = runCatching { runner().verify(wrong) }.exceptionOrNull()
        assertTrue(e is WrongPhraseException)
        assertEquals("Recovery phrase doesn't match this backup.", e!!.message)
        assertTrue(store.gets.none { it.startsWith("entries/") || it.startsWith("photos/") })
    }

    @Test
    fun restoresEntriesTagsAndPhotos() = runTest {
        val (photo, text) = backupSample()
        val r = runner()
        val progress = r.restore(r.verify(phrase)).toList()
        assertEquals(progress.last().total, progress.last().done)
        val day = target.repo.observeDayFirst("2026-10-25")
        assertEquals(setOf(photo, text), day.map { it.entry.id }.toSet())
        val p = target.repo.get(photo)!!
        assertEquals("First coffee", p.entry.text)
        assertEquals(listOf("office"), p.tags.map { it.name })
        assertTrue(target.photos.file(p.entry.photoFile!!).exists())
        assertTrue(target.photos.file(p.entry.thumbFile!!).exists())
        assertEquals(listOf("gym"), target.repo.get(text)!!.tags.map { it.name })
    }

    @Test
    fun skipsTombstones() = runTest {
        backupSample()
        val r = runner()
        r.restore(r.verify(phrase)).toList()
        assertEquals(2, target.repo.observeDayFirst("2026-10-25").size)
    }

    @Test
    fun mergeNewerWins() = runTest {
        val (_, text) = backupSample()
        val r = runner()
        r.restore(r.verify(phrase)).toList()
        target.repo.update(text, "Edited locally", null, null)
        r.restore(r.verify(phrase)).toList()
        assertEquals("Edited locally", target.repo.get(text)!!.entry.text)
    }

    @Test
    fun restoredEntriesNotReuploaded() = runTest {
        backupSample()
        val r = runner()
        r.restore(r.verify(phrase)).toList()
        assertTrue(target.db.entryDao().needsBackup().isEmpty())
        assertEquals(0, target.changes)
    }

    @Test
    fun restoreFromOtherBucketLeavesRowsForUpload() = runTest {
        backupSample()
        val r = RestoreRunner(target.db, target.photos, store, phrases, markBackedUp = false)
        r.restore(r.verify(phrase)).toList()
        assertEquals(2, target.db.entryDao().needsBackup().size)
    }

    @Test
    fun emptyBucketReportsNoBackup() = runTest {
        val e = runCatching { runner().verify(phrase) }.exceptionOrNull()
        assertTrue(e is NoBackupException)
        assertNull(store.objects["meta.json.enc"])
    }

    @Test
    fun restoredTagsKeepTheirColours() = runTest {
        // "walk" takes slot 0 on the source and never reaches the target, so a fresh assignment would differ.
        source.repo.delete(source.repo.saveText("w", listOf("walk"), source.now.toEpochMilli()))
        backupSample()
        val r = runner()
        r.restore(r.verify(phrase)).toList()
        assertEquals(1, target.db.tagDao().findByName("office")!!.colourSlot)
        assertEquals(2, target.db.tagDao().findByName("gym")!!.colourSlot)
    }
}
