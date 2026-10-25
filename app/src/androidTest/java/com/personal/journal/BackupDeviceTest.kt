package com.personal.journal

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personal.journal.backup.BackupRunner
import com.personal.journal.backup.CredentialStore
import com.personal.journal.backup.R2Client
import com.personal.journal.backup.R2Credentials
import com.personal.journal.backup.RecoveryPhrase
import com.personal.journal.backup.RestoreRunner
import com.personal.journal.backup.deriveMasterKey
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.PhotoStore
import com.personal.journal.data.db.DateSource
import com.personal.journal.data.db.JournalDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/** Runs on a device/emulator: real Keystore, real SQLite, real HTTP against an in-process fake S3. */
@RunWith(AndroidJUnit4::class)
class BackupDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val objects = ConcurrentHashMap<String, ByteArray>()
    private val server = MockWebServer()
    private val dbs = mutableListOf<JournalDatabase>()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val url = request.requestUrl!!
                val key = url.pathSegments.drop(1).joinToString("/")
                return when {
                    request.getHeader("Authorization")?.startsWith("AWS4-HMAC-SHA256 ") != true -> MockResponse().setResponseCode(403)
                    request.method == "PUT" -> { objects[key] = request.body.readByteArray(); MockResponse() }
                    request.method == "DELETE" -> { objects.remove(key); MockResponse().setResponseCode(204) }
                    request.method == "GET" && url.queryParameter("list-type") != null -> {
                        val prefix = url.queryParameter("prefix").orEmpty()
                        val keys = objects.keys.filter { it.startsWith(prefix) }.sorted()
                        MockResponse().setBody("<ListBucketResult><IsTruncated>false</IsTruncated>" + keys.joinToString("") { "<Contents><Key>$it</Key></Contents>" } + "</ListBucketResult>")
                    }
                    request.method == "GET" -> objects[key]?.let { MockResponse().setBody(Buffer().write(it)) }
                        ?: MockResponse().setResponseCode(404).setBody("<Error><Code>NoSuchKey</Code></Error>")
                    else -> MockResponse().setResponseCode(400)
                }
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
        dbs.forEach { it.close() }
        CredentialStore(context).clear()
    }

    private data class Journal(val db: JournalDatabase, val photos: PhotoStore, val repo: JournalRepository)

    private fun journal(name: String): Journal {
        val db = Room.inMemoryDatabaseBuilder(context, JournalDatabase::class.java).build().also { dbs += it }
        val photos = PhotoStore(File(context.cacheDir, "device-test-$name-${System.nanoTime()}").apply { mkdirs() })
        return Journal(db, photos, JournalRepository(db, photos))
    }

    private fun jpeg(): ByteArray {
        val bmp = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(90, 120, 160)) }
        return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
    }

    @Test
    fun credentialStoreRoundTripsThroughKeystore() {
        val store = CredentialStore(context)
        val creds = R2Credentials("acct", "key-id", "s3cr3t/with+chars", "journal")
        val key = deriveMasterKey(ByteArray(16) { 4 })
        store.saveR2(creds)
        store.saveMasterKey(key)
        store.savePhrase(listOf("legal", "winner"))
        assertEquals(creds, store.loadR2())
        assertTrue(key.encoded.contentEquals(store.loadMasterKey()!!.encoded))
        assertEquals(listOf("legal", "winner"), store.loadPhrase())
        assertTrue(store.isConnected())
        // Stored values are ciphertext, not readable as plain text.
        val stored = context.getSharedPreferences("backup_secrets", android.content.Context.MODE_PRIVATE).all.values.joinToString()
        assertTrue(stored.isNotEmpty() && !stored.contains("s3cr3t") && !stored.contains("winner"))
    }

    @Test
    fun backupThenRestoreOverHttp() = runTest {
        val phrases = RecoveryPhrase(context.resources.openRawResource(R.raw.bip39_english).bufferedReader().readLines().filter { it.isNotBlank() })
        val words = phrases.generate(SecureRandom())
        val key = deriveMasterKey(phrases.toEntropy(words))
        val r2 = R2Client(OkHttpClient(), R2Credentials("acct", "AKID", "SECRET", "journal"), endpoint = server.url("/"))

        val a = journal("a")
        val photo = a.repo.savePhoto(jpeg().inputStream(), "Blue hour", listOf("walk"), System.currentTimeMillis(), DateSource.CAMERA)
        a.repo.saveText("Leg day", listOf("gym"), System.currentTimeMillis())
        val report = BackupRunner(a.db, a.photos, r2, key).runOnce()
        assertEquals(2, report.uploaded)
        assertTrue(objects.keys.all { it == "meta.json.enc" || it.startsWith("entries/") || it.startsWith("photos/") })

        val b = journal("b")
        val restore = RestoreRunner(b.db, b.photos, r2, phrases)
        val progress = restore.restore(restore.verify(words)).toList()
        assertEquals(2, progress.last().done)
        val restored = b.repo.get(photo)!!
        assertEquals("Blue hour", restored.entry.text)
        assertEquals(listOf("walk"), restored.tags.map { it.name })
        assertTrue(b.repo.photoFile(restored.entry.photoFile!!).length() > 0)
        assertEquals(2, b.repo.observeLiveCount().first())
        assertTrue(b.db.entryDao().needsBackup().isEmpty())
    }
}
