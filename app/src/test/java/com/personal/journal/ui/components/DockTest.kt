package com.personal.journal.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.personal.journal.ui.theme.JournalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DockTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun dockActionsAreLabelledAndClickable() {
        var wrote = false
        rule.setContent {
            JournalTheme(darkTheme = false) {
                Dock(
                    listOf(
                        DockAction(JournalIcons.Gallery, "Import photos", {}),
                        DockAction(JournalIcons.Camera, "Take a photo", {}, primary = true),
                        DockAction(JournalIcons.Pen, "Write", { wrote = true }),
                    ),
                )
            }
        }
        rule.onNodeWithContentDescription("Import photos").assertExists()
        rule.onNodeWithContentDescription("Take a photo").assertExists()
        rule.onNodeWithContentDescription("Write").performClick()
        assertTrue(wrote)
    }

    @Test
    fun eachActionInvokesItsOwnCallback() {
        val fired = mutableListOf<String>()
        rule.setContent {
            JournalTheme(darkTheme = false) {
                Dock(
                    listOf(
                        DockAction(JournalIcons.Gallery, "Import photos", { fired += "import" }),
                        DockAction(JournalIcons.Camera, "Take a photo", { fired += "camera" }, primary = true),
                        DockAction(JournalIcons.Pen, "Write", { fired += "write" }),
                    ),
                )
            }
        }
        rule.onNodeWithContentDescription("Take a photo").performClick()
        rule.onNodeWithContentDescription("Write").performClick()
        rule.onNodeWithContentDescription("Import photos").performClick()
        assertEquals(listOf("camera", "write", "import"), fired)
    }
}
