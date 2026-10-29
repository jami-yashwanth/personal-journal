package com.personal.journal.ui.month

import com.personal.journal.bento.TileSize
import com.personal.journal.data.db.DateSource
import com.personal.journal.data.db.EntryEntity
import com.personal.journal.data.db.EntryType
import com.personal.journal.data.db.EntryWithTags
import com.personal.journal.data.db.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class MonthStateTest {
    private val october = YearMonth.of(2026, 10)
    private var n = 0

    private fun e(day: Int, photo: Boolean = true, text: String? = null, vararg tags: String) = EntryWithTags(
        EntryEntity(
            id = "e${n++}", type = if (photo) EntryType.PHOTO else EntryType.TEXT, text = text,
            photoFile = if (photo) "photos/x.jpg" else null, thumbFile = if (photo) "thumbs/e$n.jpg" else null,
            takenAt = n.toLong(), localDate = "2026-10-%02d".format(day), dateSource = DateSource.CAMERA,
            createdAt = 0, updatedAt = 0, deletedAt = null, backedUpAt = null,
        ),
        tags.map { TagEntity(it, it, 1, 1) },
    )

    private val sample = listOf(
        e(25), e(25), e(25, photo = false, text = "Lunch"),
        e(24, photo = false, text = "Slow Saturday"),
        e(22, tags = arrayOf("gym")), e(18, tags = arrayOf("gym")), e(16, tags = arrayOf("gym")),
        e(21, photo = false, text = "Interview", tags = arrayOf("office")),
    )

    @Test
    fun subtitleCounts() {
        val s = buildMonthUi(october, sample, today = "2026-10-25", activeTag = null, filterTags = listOf("gym", "office"))
        assertEquals("6 days, 8 entries", s.subtitle)
        assertEquals(6, s.tiles.size)
    }

    @Test
    fun filterDimsButKeepsLayout() {
        val all = buildMonthUi(october, sample, "2026-10-25", null, listOf("gym"))
        val gym = buildMonthUi(october, sample, "2026-10-25", "gym", listOf("gym"))
        assertEquals(all.tiles.map { it.placed }, gym.tiles.map { it.placed })
        val lit = gym.tiles.filter { !it.dimmed }.map { it.placed.localDate }.toSet()
        assertEquals(setOf("2026-10-22", "2026-10-18", "2026-10-16"), lit)
        assertEquals("gym on 3 days this month", gym.subtitle)
    }

    @Test
    fun todayFlagged() {
        val s = buildMonthUi(october, sample, "2026-10-25", null, emptyList())
        val today = s.tiles.single { it.isToday }
        assertEquals("2026-10-25", today.placed.localDate)
        assertEquals(TileSize.LARGE, today.placed.size)
        assertEquals("Today, 25", today.label)
        assertEquals(listOf(TileItem.Photo("thumbs/e1.jpg"), TileItem.Note("Lunch", emptyList()), TileItem.Photo("thumbs/e2.jpg")), today.items)
        assertEquals(0, today.hiddenCount)
    }

    @Test
    fun notesAreNotCrowdedOutByPhotos() {
        val day = (1..5).map { e(3) } + e(3, photo = false, text = "Hike to the falls")
        val tile = buildMonthUi(october, day, "2026-10-25", null, emptyList()).tiles.single()
        assertEquals(TileSize.LARGE, tile.placed.size)
        assertEquals(TileItem.Note("Hike to the falls", emptyList()), tile.items[1])
    }

    @Test
    fun captionsShowAsNotes() {
        val tile = buildMonthUi(october, listOf(e(7, text = "Sunday cricket")), "2026-10-25", null, emptyList()).tiles.single()
        assertEquals(listOf(TileItem.Photo("thumbs/e${n}.jpg"), TileItem.Note("Sunday cricket", emptyList())), tile.items)
    }

    @Test
    fun tilesShowWhatFitsAndCountTheRest() {
        val small = buildMonthUi(october, listOf(e(5), e(5, photo = false, text = "a"), e(5, photo = false, text = "b")), "2026-10-25", null, emptyList())
            .tiles.single()
        assertEquals(TileSize.SMALL, small.placed.size)
        assertEquals(2, small.items.size)
        assertEquals(1, small.hiddenCount)

        val large = buildMonthUi(october, (1..9).map { e(6) }, "2026-10-25", null, emptyList()).tiles.single()
        assertEquals(6, large.items.size)
        assertEquals(3, large.hiddenCount)
    }

    @Test
    fun emptyMonthState() {
        val s = buildMonthUi(october, emptyList(), "2026-10-25", null, emptyList())
        assertTrue(s.isEmpty)
        assertEquals("Nothing logged yet", s.subtitle)
    }

    @Test
    fun singularCounts() {
        val s = buildMonthUi(october, listOf(e(3)), "2026-10-25", null, emptyList())
        assertEquals("1 day, 1 entry", s.subtitle)
        assertFalse(s.tiles.single().isToday)
    }

    @Test
    fun noteCarriesItsTags() {
        val tile = buildMonthUi(october, listOf(e(7, photo = false, text = "Nets at six", "office", "cricket")), "2026-10-25", null, emptyList()).tiles.single()
        assertEquals(listOf(TileItem.Note("Nets at six", listOf("office", "cricket"))), tile.items)
    }
}
