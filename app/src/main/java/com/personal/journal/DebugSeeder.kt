package com.personal.journal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.db.DateSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Debug builds only: fills the journal with a realistic sample month for screenshots. */
object DebugSeeder {
    private data class Shot(val top: Int, val bottom: Int, val glow: Int)

    private val scenes = listOf(
        Shot(0xFF1C2740.toInt(), 0xFF5F7FA6.toInt(), 0x80BED7FF.toInt()),
        Shot(0xFF6B7581.toInt(), 0xFF2A3038.toInt(), 0x60EEF0F2),
        Shot(0xFF121A33.toInt(), 0xFFB9C9E2.toInt(), 0x90E8EEF8.toInt()),
        Shot(0xFF241C33.toInt(), 0xFF6A4A7A.toInt(), 0x80FFB4C8.toInt()),
        Shot(0xFF9EC3DC.toInt(), 0xFF24391F.toInt(), 0x403F6B3A),
        Shot(0xFFA9C9DE.toInt(), 0xFFBDB9AF.toInt(), 0x605F9BB6),
        Shot(0xFF7A838D.toInt(), 0xFF2B3137.toInt(), 0x30FFFFFF),
    )

    private fun photo(i: Int, portrait: Boolean = false): ByteArrayInputStream {
        val s = scenes[i % scenes.size]
        val (w, h) = if (portrait) 900f to 1200f else 1200f to 900f
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, s.top, s.bottom, Shader.TileMode.CLAMP) })
        c.drawCircle(w * 0.65f, h * 0.4f, 320f, Paint().apply { shader = RadialGradient(w * 0.65f, h * 0.4f, 320f, s.glow, 0, Shader.TileMode.CLAMP) })
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        return ByteArrayInputStream(out.toByteArray())
    }

    suspend fun seed(repo: JournalRepository, zone: ZoneId = ZoneId.systemDefault()) {
        val today = LocalDate.now(zone)
        fun at(daysAgo: Long, h: Int, m: Int) = today.minusDays(daysAgo).atTime(LocalTime.of(h, m)).atZone(zone).toInstant().toEpochMilli()
        var i = 0
        suspend fun p(daysAgo: Long, h: Int, m: Int, caption: String? = null, vararg tags: String, portrait: Boolean = false) =
            repo.savePhoto(photo(i++, portrait), caption, tags.toList(), at(daysAgo, h, m), DateSource.CAMERA)
        suspend fun t(daysAgo: Long, h: Int, m: Int, text: String, vararg tags: String) =
            repo.saveText(text, tags.toList(), at(daysAgo, h, m))

        p(0, 8, 42, "First coffee before standup. Office was still empty.", "office")
        t(0, 13, 15, "Lunch with Ravi and Meera at the dosa place near the station. Ravi finally said yes to the Coorg trip in December, so now someone has to actually book it.")
        p(0, 18, 30, null, "badminton", portrait = true)
        t(1, 11, 20, "Slow Saturday. Cleaned the flat, did laundry, and finally replaced the kitchen bulb that's been dead since August.")
        p(2, 19, 5, "Dinner with the team.")
        p(3, 21, 40, null, "gym")
        t(4, 18, 0, "Interview went better than I expected. They asked about the side projects first.", "office")
        repeat(4) { p(6, 9 + it, 10, if (it == 0) "Sunday cricket at the ground." else null, "cricket", portrait = it == 2) }
        p(7, 7, 30, null, "gym")
        p(9, 7, 15, null, "gym")
        t(10, 20, 0, "Rain all day. Stayed in.")
    }
}
