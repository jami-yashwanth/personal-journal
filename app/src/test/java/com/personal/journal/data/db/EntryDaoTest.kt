package com.personal.journal.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EntryDaoTest {
    private lateinit var db: JournalDatabase
    private val entries get() = db.entryDao()
    private val tags get() = db.tagDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), JournalDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun entry(id: String, day: String, takenAt: Long, deletedAt: Long? = null, backedUpAt: Long? = null, updatedAt: Long = 10) =
        EntryEntity(
            id = id, type = EntryType.TEXT, text = "t$id", photoFile = null, thumbFile = null,
            takenAt = takenAt, localDate = day, dateSource = DateSource.MANUAL,
            createdAt = 1, updatedAt = updatedAt, deletedAt = deletedAt, backedUpAt = backedUpAt,
        )

    @Test
    fun monthExcludesDeleted() = runTest {
        entries.upsert(entry("a", "2026-10-01", 1))
        entries.upsert(entry("b", "2026-10-02", 2, deletedAt = 5))
        entries.upsert(entry("c", "2026-09-30", 3))
        val month = entries.observeMonth("2026-10").first()
        assertEquals(listOf("a"), month.map { it.entry.id })
    }

    @Test
    fun dayOrderedByTime() = runTest {
        entries.upsert(entry("late", "2026-10-25", 300))
        entries.upsert(entry("early", "2026-10-25", 100))
        val day = entries.observeDay("2026-10-25").first()
        assertEquals(listOf("early", "late"), day.map { it.entry.id })
    }

    @Test
    fun needsBackupIncludesTombstones() = runTest {
        entries.upsert(entry("new", "2026-10-25", 1))
        entries.upsert(entry("done", "2026-10-25", 2, backedUpAt = 10, updatedAt = 10))
        entries.upsert(entry("changed", "2026-10-25", 3, backedUpAt = 5, updatedAt = 10))
        entries.upsert(entry("gone", "2026-10-25", 4, deletedAt = 9, backedUpAt = 5, updatedAt = 9))
        assertEquals(setOf("new", "changed", "gone"), entries.needsBackup().map { it.id }.toSet())
    }

    @Test
    fun countTodayIgnoresDeleted() = runTest {
        entries.upsert(entry("a", "2026-10-25", 1))
        entries.upsert(entry("b", "2026-10-25", 2, deletedAt = 3))
        assertEquals(1, entries.countOn("2026-10-25"))
    }

    @Test
    fun pickerOrdering() = runTest {
        tags.insert(TagEntity("1", "office", useCount = 4, lastUsedAt = 50))
        tags.insert(TagEntity("2", "gym", useCount = 9, lastUsedAt = 10))
        tags.insert(TagEntity("3", "cricket", useCount = 1, lastUsedAt = 1))
        tags.insert(TagEntity("4", "badminton", useCount = 4, lastUsedAt = 90))
        val ordered = tags.orderedForPicker(listOf("cricket")).map { it.name }
        assertEquals(listOf("cricket", "gym", "badminton", "office"), ordered)
    }

    @Test
    fun entryTagsAndMonthTagList() = runTest {
        entries.upsert(entry("a", "2026-10-01", 1))
        tags.insert(TagEntity("g", "gym", 1, 1))
        tags.insert(TagEntity("o", "office", 1, 1))
        tags.setTagsFor("a", listOf("g", "o"))
        val loaded = entries.getById("a")!!
        assertEquals(setOf("gym", "office"), loaded.tags.map { it.name }.toSet())
        tags.setTagsFor("a", listOf("g"))
        assertEquals(listOf("gym"), entries.getById("a")!!.tags.map { it.name })
        assertEquals(listOf("gym"), tags.tagsUsedInMonth("2026-10").map { it.name })
        assertEquals(listOf("gym"), tags.search("gy").map { it.name })
    }
}
