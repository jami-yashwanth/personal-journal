package com.personal.journal.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val name = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), JournalDatabase::class.java)

    private fun v1WithTags(vararg names: String) = helper.createDatabase(name, 1).apply {
        names.forEachIndexed { i, tag -> execSQL("INSERT INTO tags (id, name, useCount, lastUsedAt) VALUES ('t$i', '$tag', 1, 1)") }
        close()
    }

    private fun slotsByName(): Map<String, Int> {
        val db = helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)
        return db.query("SELECT name, colourSlot FROM tags").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getInt(1)) }
        }
    }

    @Test
    fun existingTagsGetDistinctSlotsInNameOrder() {
        v1WithTags("office", "family", "gym", "cricket")
        assertEquals(mapOf("cricket" to 0, "family" to 1, "gym" to 2, "office" to 3), slotsByName())
    }

    @Test
    fun moreTagsThanColoursWrap() {
        v1WithTags("a", "b", "c", "d", "e", "f", "g", "h")
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 0, 1), slotsByName().toSortedMap().values.toList())
    }

    @Test
    fun migratedDatabaseOpensAndKeepsAssigning() = runTest {
        v1WithTags("gym", "office")
        helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).close()
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), JournalDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
        val walk = db.tagDao().create("t9", "walk", lastUsedAt = 2)
        assertEquals(2, walk.colourSlot)
        db.close()
    }
}
