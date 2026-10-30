package com.personal.journal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.personal.journal.R

private fun outfit(weight: FontWeight) = Font(
    R.font.outfit,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Outfit = FontFamily(
    outfit(FontWeight.Normal), outfit(FontWeight.Medium), outfit(FontWeight.SemiBold), outfit(FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, line: Float = 1.4f, spacing: Float? = null) = TextStyle(
    fontFamily = Outfit, fontSize = size.sp, fontWeight = weight, lineHeight = (size * line).sp,
    letterSpacing = spacing?.em ?: TextUnit.Unspecified,
)

/** Text printed on a polaroid slip, scaled to the slip's width. */
fun slipStyle(size: TextUnit) = TextStyle(
    fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = size, lineHeight = size * 1.2f,
)

val JournalTypography = Typography(
    displayLarge = style(42, FontWeight.Bold, 1.0f, -0.025f),
    headlineLarge = style(28, FontWeight.Medium, 1.35f),
    headlineMedium = style(26, FontWeight.Medium, 1.2f),
    headlineSmall = style(24, FontWeight.Medium, 1.2f),
    titleLarge = style(20, FontWeight.Medium),
    titleMedium = style(17, FontWeight.Medium),
    bodyLarge = style(16, FontWeight.Medium, 1.38f),
    bodyMedium = style(15, FontWeight.Normal, 1.45f),
    bodySmall = style(14, FontWeight.Normal),
    labelLarge = style(15, FontWeight.SemiBold),
    labelMedium = style(13, FontWeight.Medium),
    labelSmall = style(12, FontWeight.Medium),
)
