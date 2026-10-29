package com.personal.journal.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import com.personal.journal.Routes

/** How a pair of screens hands over: a shared element carries it, a sideways push, or the camera's vertical sheet. */
enum class NavMotion { Shared, Push, Sheet }

private val sheetPair: Set<String?> = setOf(Routes.CAPTURE, Routes.MONTH)
private val sharedPairs: List<Set<String?>> = listOf(setOf(Routes.MONTH, Routes.DAY), setOf(Routes.DAY, Routes.ENTRY))

/** Same answer in both directions, so a pop reverses its push. */
fun navMotion(from: String?, to: String?): NavMotion {
    val pair = setOf(from, to)
    return when (pair) {
        sheetPair -> NavMotion.Sheet
        in sharedPairs -> NavMotion.Shared
        else -> NavMotion.Push
    }
}

/** The one spring screens and shared elements move on; no bounce, so pages settle rather than wobble. */
fun <T> navSpring(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
