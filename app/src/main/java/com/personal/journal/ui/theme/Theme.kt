package com.personal.journal.ui.theme

import androidx.compose.foundation.LocalIndication
import com.personal.journal.ui.motion.PressScaleIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object JournalColors {
    val Background @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.background
    val Card @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.card
    val Sunk @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.sunk
    val Line @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.line
    val TextPrimary @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.text
    val TextSecondary @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.textSecondary
    val TextMuted @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.textMuted
    val Action @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.action
    val OnAction @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.onAction
    val Paper @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.paper
    val OnPaper @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.onPaper
    val OnColour @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.onColour
    val Danger @Composable @ReadOnlyComposable get() = LocalJournalPalette.current.danger
}

object JournalShapes {
    val tile = RoundedCornerShape(24.dp)
    val card = RoundedCornerShape(24.dp)
    val button = RoundedCornerShape(16.dp)
    val photo = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(50)
    val hero = RoundedCornerShape(40.dp)
    val print = RoundedCornerShape(7.dp)
}

private fun JournalPalette.toColorScheme() =
    if (isDark) {
        darkColorScheme(
            primary = action, onPrimary = onAction,
            background = background, onBackground = text,
            surface = card, onSurface = text,
            surfaceVariant = sunk, onSurfaceVariant = textSecondary,
            surfaceContainer = card, surfaceContainerHigh = sunk,
            outline = line, error = danger,
        )
    } else {
        lightColorScheme(
            primary = action, onPrimary = onAction,
            background = background, onBackground = text,
            surface = card, onSurface = text,
            surfaceVariant = sunk, onSurfaceVariant = textSecondary,
            surfaceContainer = card, surfaceContainerHigh = sunk,
            outline = line, error = danger,
        )
    }

@Composable
fun JournalTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalJournalPalette provides palette) {
        MaterialTheme(colorScheme = palette.toColorScheme(), typography = JournalTypography) {
            // Inside MaterialTheme, which would otherwise set the ripple.
            CompositionLocalProvider(LocalIndication provides PressScaleIndication, content = content)
        }
    }
}
