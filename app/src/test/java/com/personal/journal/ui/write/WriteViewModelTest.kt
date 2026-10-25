package com.personal.journal.ui.write

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
class WriteViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var j: TestJournal

    @Before fun setUp() { j = TestJournal(tmp.root) }
    @After fun tearDown() = j.close()

    @Test
    fun writeBlankTextCannotSave() = runTest {
        val vm = WriteViewModel(j.repo)
        vm.updateText("   ")
        assertFalse(vm.canSave)
        assertNull(vm.save())
    }

    @Test
    fun writeUnsavedFlag() {
        val vm = WriteViewModel(j.repo)
        assertFalse(vm.hasUnsavedText)
        vm.updateText("Long day")
        assertTrue(vm.hasUnsavedText)
    }

    @Test
    fun savesWithTagsAndChosenTime() = runTest {
        val vm = WriteViewModel(j.repo)
        vm.updateText("Leg day")
        vm.tags.toggle("gym")
        val t = j.millis("2026-10-24T20:00:00+05:30")
        vm.updateTakenAt(t)
        val id = vm.save()!!
        val e = j.repo.get(id)!!
        assertEquals("2026-10-24", e.entry.localDate)
        assertEquals(listOf("gym"), e.tags.map { it.name })
        assertFalse(vm.hasUnsavedText)
    }
}
