package com.personal.journal.ui.motion

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneShotTest {
    @Test fun consumesOnce() {
        val handle = SavedStateHandle(mapOf(JustSaved to "/p.jpg"))
        assertEquals("/p.jpg", handle.consumeOnce(JustSaved))
        assertNull(handle.consumeOnce(JustSaved))
    }
}
