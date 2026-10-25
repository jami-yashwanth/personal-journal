package com.personal.journal.testing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.PhotoStore
import com.personal.journal.data.db.JournalDatabase
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** In-memory journal for tests. Times are in IST; "now" is 2026-10-25 23:59 IST by default. */
class TestJournal(
    val root: File,
    val now: Instant = OffsetDateTime.parse("2026-10-25T23:59:00+05:30").toInstant(),
    var zone: ZoneId = ZoneId.of("Asia/Kolkata"),
) {
    val db: JournalDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), JournalDatabase::class.java)
        .allowMainThreadQueries().build()
    val photos = PhotoStore(File(root, "files").apply { mkdirs() })
    var changes = 0
    val repo = JournalRepository(db, photos, Clock.fixed(now, ZoneOffset.UTC), { zone }, onChanged = { changes++ })

    fun jpegFile(name: String = "src-${System.nanoTime()}.jpg", w: Int = 40, h: Int = 30): File {
        val f = File(root, name)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return f
    }

    fun millis(iso: String): Long = OffsetDateTime.parse(iso).toInstant().toEpochMilli()

    fun close() = db.close()
}

suspend fun JournalRepository.observeDayFirst(date: String) = observeDay(date).first()
