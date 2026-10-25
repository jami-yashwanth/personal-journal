package com.personal.journal.ui.entry

import com.personal.journal.testing.TestJournal
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
}
