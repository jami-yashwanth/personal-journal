package com.personal.journal.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/** The layout every shared element flies within; null outside the nav host (previews, tests). */
val LocalSharedScope: ProvidableCompositionLocal<SharedTransitionScope?> = staticCompositionLocalOf { null }

/** The entering or leaving screen's animation, so a shared element knows which side of the hand-over it is on. */
val LocalNavScope: ProvidableCompositionLocal<AnimatedVisibilityScope?> = staticCompositionLocalOf { null }

fun dayKey(date: String): String = "day-$date"
fun photoKey(entryId: String): String = "photo-$entryId"
fun noteKey(entryId: String): String = "note-$entryId"

private val springBounds = BoundsTransform { _, _ -> navSpring() }

/** A container that grows into, or shrinks back from, the matching container on the next screen; its contents cross-fade. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedTile(key: String, shape: Shape): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val nav = LocalNavScope.current ?: return this
    return with(shared) {
        this@sharedTile.sharedBounds(
            rememberSharedContentState(key),
            nav,
            boundsTransform = springBounds,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}

/** The same picture on both screens, carried from one place to the other. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedPhoto(key: String, shape: Shape): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val nav = LocalNavScope.current ?: return this
    return with(shared) {
        this@sharedPhoto.sharedElement(
            rememberSharedContentState(key),
            nav,
            boundsTransform = springBounds,
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}

/** Gives a destination's content its own side of the nav transition. */
@Composable
fun NavScoped(scope: AnimatedVisibilityScope, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavScope provides scope, content = content)
}

/** Only the page the pager has settled on takes part, so the side peeks stay put. */
fun sharedKeyFor(page: Int, settledPage: Int, key: String): String? = key.takeIf { page == settledPage }

/**
 * For a detail drawn on top of a shared photo (a time sticker): the flying photo covers it, so it steps aside while
 * anything is in flight and fades back once the photo has landed, instead of popping in.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.besideSharedFlight(): Modifier {
    val flying = LocalSharedScope.current?.isTransitionActive == true
    val alpha by animateFloatAsState(if (flying) 0f else 1f, navSpring(), label = "beside shared flight")
    return graphicsLayer { this.alpha = alpha }
}
