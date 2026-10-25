package com.personal.journal.backup

import com.personal.journal.data.db.DateSource
import com.personal.journal.testing.TestJournal
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupRunnerTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var j: TestJournal
    private val store = FakeObjectStore()
    private val key = deriveMasterKey(ByteArray(16) { 3 })
    private lateinit var runner: BackupRunner

    @Before
    fun setUp() {
        j = TestJournal(tmp.root)
        runner = BackupRunner(j.db, j.photos, store, key)
    }

    @After fun tearDown() = j.close()

    private suspend fun photoEntry(caption: String = "First coffee") =
        j.jpegFile().inputStream().use { j.repo.savePhoto(it, caption, listOf("office"), j.now.toEpochMilli(), DateSource.CAMERA) }

    @Test
    fun uploadsPhotoThenJsonThenMarks() = runTest {
        val id = photoEntry()
        val report = runner.runOnce()
        assertEquals(1, report.uploaded)
        assertNotNull(store.objects[BackupCodec.photoKey(id)])
        val json = BackupCodec.decode(Envelope.open(key, store.objects[BackupCodec.entryKey(id)]!!, BackupCodec.entryKey(id)))
        assertEquals("First coffee", json.text)
        assertEquals(listOf("office"), json.tags)
        assertTrue(j.db.entryDao().needsBackup().isEmpty())
        assertTrue(KeyCheck.matches(key, store.objects["meta.json.enc"]!!))
    }

    @Test
    fun interruptedAfterPhotoRetriesWholeEntry() = runTest {
        val id = photoEntry()
        store.failPut += BackupCodec.entryKey(id)
        val first = runner.runOnce()
        assertEquals(1, first.failed)
        assertEquals(BackupErrorKind.NETWORK, first.lastError)
        assertEquals(listOf(id), j.db.entryDao().needsBackup().map { it.id })
        val second = runner.runOnce()
        assertEquals(1, second.uploaded)
        assertNotNull(store.objects[BackupCodec.entryKey(id)])
        assertTrue(j.db.entryDao().needsBackup().isEmpty())
    }

    @Test
    fun tombstoneDeletesPhotoAndPurges() = runTest {
        val id = photoEntry()
        runner.runOnce()
        j.repo.delete(id)
        val report = runner.runOnce()
        assertEquals(1, report.deleted)
        assertNull(store.objects[BackupCodec.photoKey(id)])
        val tomb = BackupCodec.decode(Envelope.open(key, store.objects[BackupCodec.entryKey(id)]!!, BackupCodec.entryKey(id)))
        assertNotNull(tomb.deletedAt)
        assertNull(j.db.entryDao().getById(id))
    }

    @Test
    fun oneFailureDoesNotBlockOthers() = runTest {
        val a = j.repo.saveText("a", emptyList(), j.now.toEpochMilli())
        val b = j.repo.saveText("b", emptyList(), j.now.toEpochMilli())
        store.failPut += BackupCodec.entryKey(a)
        val report = runner.runOnce()
        assertEquals(1, report.uploaded)
        assertEquals(1, report.failed)
        assertNotNull(store.objects[BackupCodec.entryKey(b)])
    }

    @Test
    fun objectsAreEncrypted() = runTest {
        val id = photoEntry(caption = "secret caption")
        runner.runOnce()
        val raw = store.objects[BackupCodec.entryKey(id)]!!
        assertEquals(Envelope.VERSION, raw[0])
        assertFalse(String(raw, Charsets.ISO_8859_1).contains("secret caption"))
    }

    @Test
    fun objectNamesHaveNoContent() = runTest {
        photoEntry(caption = "secret caption")
        j.repo.saveText("text", listOf("gym"), j.now.toEpochMilli())
        runner.runOnce()
        val pattern = Regex("^(meta\\.json\\.enc|entries/[0-9a-f-]{36}\\.json\\.enc|photos/[0-9a-f-]{36}\\.jpg\\.enc)$")
        assertTrue(store.objects.keys.toString(), store.objects.keys.all { pattern.matches(it) })
    }

    @Test
    fun foreignBackupIsRefused() = runTest {
        store.objects["meta.json.enc"] = KeyCheck.create(deriveMasterKey(ByteArray(16) { 9 }))
        j.repo.saveText("a", emptyList(), j.now.toEpochMilli())
        val report = runner.runOnce()
        assertEquals(BackupErrorKind.WRONG_KEY, report.lastError)
        assertEquals(0, report.uploaded)
    }

    @Test
    fun editAfterBackupIsReuploaded() = runTest {
        val id = j.repo.saveText("a", emptyList(), j.now.toEpochMilli())
        runner.runOnce()
        j.repo.update(id, "b", null, null)
        assertEquals(listOf(id), j.db.entryDao().needsBackup().map { it.id })
        runner.runOnce()
        val json = BackupCodec.decode(Envelope.open(key, store.objects[BackupCodec.entryKey(id)]!!, BackupCodec.entryKey(id)))
        assertEquals("b", json.text)
    }
}
