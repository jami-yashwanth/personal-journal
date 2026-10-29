package com.personal.journal.ui.entry

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.personal.journal.testing.TestJournal
import com.personal.journal.ui.theme.JournalTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class EntryScreenTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var j: TestJournal

    @Before fun setUp() { j = TestJournal(tmp.root) }
    @After fun tearDown() = j.close()

    @Test
    fun swipeToLastPageDeleteLeavesTwoOfTwo() {
        val ids = runBlocking {
            listOf("09:00", "12:00", "18:00").mapIndexed { i, t ->
                j.repo.saveText("note ${"ABC"[i]}", emptyList(), j.millis("2026-10-25T$t:00+05:30"))
            }
        }
        val vm = EntryViewModel(j.repo, ids[0])
        var backed = false
        rule.setContent {
            JournalTheme(darkTheme = false) {
                EntryScreen(vm, photoFile = { File(tmp.root, it) }, tintCache = TintCache(), onBack = { backed = true })
            }
        }
        rule.waitUntil { vm.day.value.size == 3 }
        rule.onNodeWithText("1 of 3").assertExists()

        repeat(2) {
            rule.onRoot().performTouchInput { swipeLeft() }
            rule.waitForIdle()
        }
        rule.onNodeWithText("3 of 3").assertExists()

        rule.onNodeWithContentDescription("Delete").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.waitUntil { vm.day.value.size == 2 }

        assertNull(runBlocking { j.repo.get(ids[2]) })
        assertNotNull(runBlocking { j.repo.get(ids[1]) })
        rule.onNodeWithText("2 of 2").assertExists()
        assertEquals(false, backed)
    }
}
