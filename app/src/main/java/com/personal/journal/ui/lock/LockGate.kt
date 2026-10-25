package com.personal.journal.ui.lock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Locked on cold start and after [timeoutMs] in the background. */
class LockGate(
    private val clock: () -> Long = System::currentTimeMillis,
    private val timeoutMs: Long = 60_000,
) {
    var locked by mutableStateOf(true)
        private set
    private var backgroundedAt: Long? = null

    fun onBackground() {
        backgroundedAt = clock()
    }

    fun onForeground() {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (clock() - since >= timeoutMs) locked = true
    }

    fun unlocked() {
        locked = false
    }
}
