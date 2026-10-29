package com.personal.journal.ui.entry

import com.personal.journal.testing.TestJournal
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EntryViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var j: TestJournal

    @Before fun setUp() { j = TestJournal(tmp.root) }
    @After fun tearDown() = j.close()

    @Test
    fun entryDateChangeMovesDay() = runTest {
        val id = j.repo.saveText("Leg day", listOf("gym"), j.now.toEpochMilli())
        val vm = EntryViewModel(j.repo, id)
        vm.load()
        vm.updateDate(j.millis("2026-10-20T09:00:00+05:30"))
        assertEquals("2026-10-20", j.repo.get(id)!!.entry.localDate)
        assertEquals("2026-10-20", vm.entry.value!!.entry.localDate)
    }

    @Test
    fun editSavesTextAndTags() = runTest {
        val id = j.repo.saveText("Leg day", listOf("gym"), j.now.toEpochMilli())
        val vm = EntryViewModel(j.repo, id)
        vm.load()
        vm.startEdit()
        assertTrue("gym" in vm.editTags!!.selected)
        vm.editTags!!.toggle("gym")
        vm.editTags!!.toggle("office")
        vm.saveEdit("Arm day")
        val e = j.repo.get(id)!!
        assertEquals("Arm day", e.entry.text)
        assertEquals(listOf("office"), e.tags.map { it.name })
        assertNull(vm.editTags)
    }

    @Test
    fun entryDeleteRemovesIt() = runTest {
        val id = j.repo.saveText("x", emptyList(), j.now.toEpochMilli())
        val vm = EntryViewModel(j.repo, id)
        vm.load()
        vm.delete()
        assertNull(j.repo.get(id))
    }

    /** Three notes on 2026-10-25, in time order: A, B, C. */
    private suspend fun threeOnOneDay(): List<String> = listOf("09:00", "12:00", "18:00").mapIndexed { i, t ->
        j.repo.saveText("note ${"ABC"[i]}", emptyList(), j.millis("2026-10-25T$t:00+05:30"))
    }

    @Test
    fun opensOnTappedEntry() = runTest {
        val (a, b, c) = threeOnOneDay()
        val vm = EntryViewModel(j.repo, b)
        vm.load()
        assertEquals(listOf(a, b, c), vm.day.value.map { it.entry.id })
        assertEquals(1, vm.initialIndex)
        assertEquals(1, vm.selected.value)
        assertEquals(b, vm.entry.value!!.entry.id)
        assertFalse(vm.dayIsEmpty.value)
    }

    @Test
    fun actionsFollowSelection() = runTest {
        val (_, b, c) = threeOnOneDay()
        val vm = EntryViewModel(j.repo, b)
        vm.load()
        vm.select(2)
        assertEquals(c, vm.entry.value!!.entry.id)
        vm.updateDate(j.millis("2026-10-20T09:00:00+05:30"))
        assertEquals("2026-10-20", j.repo.get(c)!!.entry.localDate)
        assertEquals("2026-10-25", j.repo.get(b)!!.entry.localDate)
        // the pager follows the moved entry into its new day
        assertEquals(c, vm.entry.value!!.entry.id)
    }

    @Test
    fun editActsOnSelection() = runTest {
        val (a, b, _) = threeOnOneDay()
        val vm = EntryViewModel(j.repo, b)
        vm.load()
        vm.select(0)
        vm.startEdit()
        vm.saveEdit("edited")
        assertEquals("edited", j.repo.get(a)!!.entry.text)
        assertEquals("note B", j.repo.get(b)!!.entry.text)
        assertEquals(a, vm.entry.value!!.entry.id)
    }

    @Test
    fun deletingLastEntryEmptiesDay() = runTest {
        val id = j.repo.saveText("only", emptyList(), j.now.toEpochMilli())
        val vm = EntryViewModel(j.repo, id)
        vm.load()
        assertFalse(vm.dayIsEmpty.value)
        vm.delete()
        assertTrue(vm.dayIsEmpty.value)
        assertNull(vm.entry.value)
    }

    @Test
    fun deleteKeepsIndexInRange() = runTest {
        val (a, b, c) = threeOnOneDay()
        val vm = EntryViewModel(j.repo, b)
        vm.load()
        vm.select(2)
        vm.delete()
        assertNull(j.repo.get(c))
        assertEquals(1, vm.selected.value)
        assertEquals(listOf(a, b), vm.day.value.map { it.entry.id })
        assertEquals(b, vm.entry.value!!.entry.id)
    }

    @Test
    fun reloadAfterDeletingOpenedEntryKeepsDay() = runTest {
        val (a, b, c) = threeOnOneDay()
        val vm = EntryViewModel(j.repo, b)
        vm.load()
        vm.delete()
        vm.load() // the screen re-entering composition, e.g. after rotation
        assertFalse(vm.dayIsEmpty.value)
        assertEquals(listOf(a, c), vm.day.value.map { it.entry.id })
        assertEquals(c, vm.entry.value!!.entry.id)
    }

    @Test
    fun reloadKeepsSelection() = runTest {
        val (_, b, c) = threeOnOneDay()
        val vm = EntryViewModel(j.repo, b)
        vm.load()
        vm.select(2)
        vm.load()
        assertEquals(2, vm.selected.value)
        assertEquals(c, vm.entry.value!!.entry.id)
    }
}
