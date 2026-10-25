package com.personal.journal.ui.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockGateTest {
    private var now = 0L
    private val gate = LockGate(clock = { now }, timeoutMs = 60_000)

    @Test
    fun coldStartLocks() {
        assertTrue(gate.locked)
        gate.unlocked()
        assertFalse(gate.locked)
    }

    @Test
    fun locksAfterTimeout() {
        gate.unlocked()
        gate.onBackground()
        now += 59_000
        gate.onForeground()
        assertFalse(gate.locked)
        gate.onBackground()
        now += 61_000
        gate.onForeground()
        assertTrue(gate.locked)
    }

    @Test
    fun foregroundWithoutBackgroundStaysUnlocked() {
        gate.unlocked()
        now += 600_000
        gate.onForeground()
        assertFalse(gate.locked)
    }
}
