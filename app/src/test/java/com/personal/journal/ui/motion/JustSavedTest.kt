package com.personal.journal.ui.motion

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JustSavedTest {
    @get:Rule val compose = createComposeRule()

    /** Leaving the camera mid-shrink (to Month, or a rotation) must not play it again on the way back. */
    @Test fun notReplayedWhenTheCameraIsRestored() {
        val handle = SavedStateHandle(mapOf(JustSaved to "id-1"))
        val restore = StateRestorationTester(compose)
        restore.setContent {
            val saved = rememberJustSaved(handle) { "/photos/$it.jpg" }
            Text(saved.value ?: "nothing")
        }
        compose.onNodeWithText("/photos/id-1.jpg").assertExists()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("nothing").assertExists()
    }
}
