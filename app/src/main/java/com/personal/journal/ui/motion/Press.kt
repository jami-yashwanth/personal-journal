package com.personal.journal.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

const val PressedScale = 0.96f

/** Quick to sink, a touch of overshoot coming back up, so a tap feels physical. */
fun <T> pressSpring(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)

/** Every tappable sinks slightly under the finger instead of rippling; provided app-wide through LocalIndication. */
object PressScaleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressScaleNode(interactionSource)
    override fun equals(other: Any?) = other === this
    override fun hashCode() = System.identityHashCode(this)
}

private class PressScaleNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private val scale = Animatable(1f)

    override fun onAttach() {
        // Subscribed before onAttach returns: clickable creates this node on the first press and emits it straight after.
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val presses = mutableListOf<PressInteraction.Press>()
            source.interactions.collect { i ->
                when (i) {
                    is PressInteraction.Press -> presses += i
                    is PressInteraction.Release -> presses -= i.press
                    is PressInteraction.Cancel -> presses -= i.press
                    else -> return@collect
                }
                val target = if (presses.isEmpty()) 1f else PressedScale
                launch { scale.animateTo(target, pressSpring()) { invalidateDraw() } }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = scale.value
        if (s == 1f) drawContent() else scale(s) { this@draw.drawContent() }
    }
}

/** A short tick for the taps that commit to something: the shutter and opening a day or an entry. */
@Composable
fun rememberTapHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.ContextClick) } }
}
