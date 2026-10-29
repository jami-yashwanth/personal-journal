package com.personal.journal.ui.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedKeysTest {
    @get:Rule val compose = createComposeRule()

    @Test fun keysAreDistinctPerKind() {
        assertNotEquals(photoKey("a"), noteKey("a"))
        assertNotEquals(photoKey("a"), photoKey("b"))
    }

    @Test fun dayKeyIsStable() {
        assertEquals("day-2026-10-30", dayKey("2026-10-30"))
    }

    @Test fun modifiersAreNoOpsWithoutScopes() {
        compose.setContent {
            Box(Modifier.testTag("t").size(10.dp).sharedTile("k", RectangleShape).sharedPhoto("p", RectangleShape))
        }
        compose.onNodeWithTag("t").assertExists()
    }
}
