package com.personal.journal.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class JournalPalette(
    val background: Color,
    val card: Color,
    val sunk: Color,
    val line: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val action: Color,
    val onAction: Color,
    val paper: Color,
    val onPaper: Color,
    val onColour: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LightPalette = JournalPalette(
    background = Color(0xFFFFF6E9),
    card = Color(0xFFFFFFFF),
    sunk = Color(0xFFF4E9DA),
    line = Color(0xFFEBDFCD),
    text = Color(0xFF1E1B19),
    textSecondary = Color(0xFF5C554E),
    textMuted = Color(0xFF8E857B),
    action = Color(0xFF1E1B19),
    onAction = Color(0xFFFFFFFF),
    paper = Color(0xFFFFFFFF),
    onPaper = Color(0xFF1E1B19),
    onColour = Color(0xFF1E1B19),
    danger = Color(0xFFF07F86),
    isDark = false,
)

val DarkPalette = JournalPalette(
    background = Color(0xFF191614),
    card = Color(0xFF24201C),
    sunk = Color(0xFF2B2520),
    line = Color(0xFF38312B),
    text = Color(0xFFF3ECE3),
    textSecondary = Color(0xFFC9BEB2),
    textMuted = Color(0xFF968A7E),
    action = Color(0xFFF3ECE3),
    onAction = Color(0xFF191614),
    paper = Color(0xFFEFE8DE),
    onPaper = Color(0xFF1E1B19),
    onColour = Color(0xFF1E1B19),
    danger = Color(0xFFF07F86),
    isDark = true,
)

val LocalJournalPalette = staticCompositionLocalOf { LightPalette }
