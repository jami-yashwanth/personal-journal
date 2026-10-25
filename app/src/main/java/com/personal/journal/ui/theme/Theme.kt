package com.personal.journal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object JournalColors {
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1C1C1C)
    val SurfaceRaised = Color(0xFF262626)
    val TextPrimary = Color(0xFFEDEDED)
    val TextSecondary = Color(0xFFBDBDBD)
    val TextMuted = Color(0xFF8C8C8C)
    val PrimaryFill = Color(0xFFEDEDED)
    val OnPrimary = Color(0xFF121212)
    val DestructiveOutline = Color(0xFF3A2522)
    val DestructiveIcon = Color(0xFFE58A80)
    val Hairline = Color(0xFF2A2A2A)
    val Scrim = Color(0xB3121212)
}

object JournalShapes {
    val tile = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(24.dp)
    val button = RoundedCornerShape(16.dp)
    val photo = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(50)
}

private val scheme = darkColorScheme(
    primary = JournalColors.PrimaryFill,
    onPrimary = JournalColors.OnPrimary,
    background = JournalColors.Background,
    onBackground = JournalColors.TextPrimary,
    surface = JournalColors.Surface,
    onSurface = JournalColors.TextPrimary,
    surfaceVariant = JournalColors.SurfaceRaised,
    onSurfaceVariant = JournalColors.TextSecondary,
    surfaceContainer = JournalColors.Surface,
    surfaceContainerHigh = JournalColors.SurfaceRaised,
    outline = JournalColors.Hairline,
    error = JournalColors.DestructiveIcon,
)

@Composable
fun JournalTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = JournalTypography, content = content)
}
