package com.personal.journal.ui.review

import com.personal.journal.data.db.DateSource
import com.personal.journal.testing.TestJournal
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var j: TestJournal

    @Before fun setUp() { j = TestJournal(tmp.root) }
    @After fun tearDown() = j.close()

    @Test
    fun reviewSaveUsesCameraSourceAndCleansTemp() = runTest {
        val temp = j.jpegFile("capture-1.jpg")
        val vm = ReviewViewModel(j.repo, temp)
        vm.tags.toggle("office")
        val id = vm.save("  First coffee  ")
        val e = j.repo.get(id)!!
        assertEquals(DateSource.CAMERA, e.entry.dateSource)
        assertEquals(j.now.toEpochMilli(), e.entry.takenAt)
        assertEquals("First coffee", e.entry.text)
        assertEquals(listOf("office"), e.tags.map { it.name })
        assertFalse(temp.exists())
    }

    @Test
    fun retakeDeletesTemp() {
        val temp = j.jpegFile("capture-2.jpg")
        ReviewViewModel(j.repo, temp).discard()
        assertFalse(temp.exists())
    }
}
