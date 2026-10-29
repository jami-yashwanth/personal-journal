package com.personal.journal.ui.motion

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import kotlinx.coroutines.runBlocking
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.personal.journal.ui.theme.JournalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PressTest {
    @get:Rule val compose = createComposeRule()

    /** The box's own top-left pixel: red at rest, the white backdrop once the box has sunk away from its corner. */
    private fun corner() = compose.onNodeWithTag("box").captureToImage().toPixelMap()[1, 1]

    private val scroll = ScrollState(0)

    private fun show() = compose.setContent {
        JournalTheme {
            Column(Modifier.size(200.dp).background(Color.White).verticalScroll(scroll)) {
                Box(Modifier.testTag("box").size(100.dp).clickable {}.background(Color.Red))
                Box(Modifier.size(600.dp))
            }
        }
    }

    @Test fun themeProvidesPressScale() {
        var indication: Any? = null
        compose.setContent { JournalTheme { indication = LocalIndication.current } }
        assertSame(PressScaleIndication, indication)
    }

    @Test fun pressScalesDown() {
        show()
        assertEquals(Color.Red, corner())
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("box").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(1000)
        assertNotEquals(Color.Red, corner())
    }

    @Test fun pressReleasesOnCancel() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("box").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(1000)
        // the drag turns into a scroll, which cancels the press
        compose.onNodeWithTag("box").performTouchInput { moveBy(Offset(0f, -400f)); up() }
        compose.mainClock.advanceTimeBy(1000)
        assertNotEquals(0, scroll.value)
        compose.runOnIdle { runBlocking { scroll.scrollTo(0) } }
        compose.mainClock.advanceTimeBy(100)
        assertEquals(Color.Red, corner())
    }
}
