package com.personal.journal.ui.day

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.personal.journal.testing.TestJournal
import com.personal.journal.ui.theme.JournalTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.personal.journal.ui.motion.ImageKeys
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DayScreenTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var j: TestJournal

    @Before fun setUp() { j = TestJournal(tmp.root) }
    @After fun tearDown() = j.close()

    @Test
    fun deletingTheLastEntryGoesBack() {
        val id = runBlocking { j.repo.saveText("only one", emptyList(), j.now.toEpochMilli()) }
        var backed = false
        rule.setContent {
            JournalTheme(darkTheme = false) {
                DayScreen(
                    DayViewModel(j.repo, "2026-10-25"), photoFile = { File(tmp.root, it) }, imageKeys = ImageKeys(),
                    onBack = { backed = true }, onOpenEntry = {}, onCamera = {}, onImport = {}, onWrite = {},
                )
            }
        }
        rule.waitUntil { runCatching { rule.onNodeWithText("only one").assertExists() }.isSuccess }
        assertFalse(backed)
        runBlocking { j.repo.delete(id) }
        rule.waitUntil { backed }
        assertTrue(backed)
    }
}
