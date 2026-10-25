package com.personal.journal.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.personal.journal.ui.theme.JournalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TagRowTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: TagRowState, onToggle: (String) -> Unit = {}) = compose.setContent {
        JournalTheme {
            TagRow(state, onToggle = onToggle, onQuery = {}, onCreate = {}, onOpenSearch = {}, onCloseSearch = {})
        }
    }

    @Test
    fun selectedRendersSelected() {
        show(TagRowState(tags = listOf("office", "gym"), selected = setOf("office")))
        compose.onNodeWithText("office").assertIsSelected()
        compose.onNodeWithText("gym").assertIsNotSelected()
        compose.onNodeWithContentDescription("Search tags").assertIsDisplayed()
    }

    @Test
    fun tapTogglesTag() {
        var toggled = ""
        show(TagRowState(tags = listOf("gym")), onToggle = { toggled = it })
        compose.onNodeWithText("gym").performClick()
        assertEquals("gym", toggled)
    }

    @Test
    fun createOfferedOnlyWithoutExactMatch() {
        assertTrue(canCreateTag("ba", listOf("badminton")))
        assertFalse(canCreateTag("badminton", listOf("badminton")))
        assertFalse(canCreateTag("  Badminton ", listOf("badminton")))
        assertFalse(canCreateTag("", emptyList()))
        assertFalse(canCreateTag("x".repeat(30), emptyList()))
    }

    @Test
    fun searchingShowsCreateAndSelected() {
        show(
            TagRowState(
                tags = listOf("office"), selected = setOf("office"), searching = true,
                query = "ba", results = listOf("badminton"), canCreate = true,
            ),
        )
        compose.onNodeWithText("badminton").assertIsDisplayed()
        compose.onNodeWithText("+ Create \"ba\"").assertIsDisplayed()
        compose.onNodeWithText("Selected").assertIsDisplayed()
    }
}
