package com.personal.journal.ui.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.personal.journal.R

private fun geist(weight: FontWeight) = Font(
    R.font.geist,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Geist = FontFamily(geist(FontWeight.Normal), geist(FontWeight.Medium), geist(FontWeight.SemiBold))

/** Handwriting for the user's own words: notes and captions, wherever they are shown (not where they are typed). */
val Caveat = FontFamily(
    Font(R.font.caveat, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
)

/** The user's words in Caveat. Handwriting runs small, so callers pick a size a step above the body text it replaces. */
fun handwriting(size: TextUnit, line: Float = 1.2f) =
    TextStyle(fontFamily = Caveat, fontWeight = FontWeight.Medium, fontSize = size, lineHeight = size * line)

private fun style(size: Int, weight: FontWeight, line: Float = 1.4f) =
    TextStyle(fontFamily = Geist, fontSize = size.sp, fontWeight = weight, lineHeight = (size * line).sp)

val JournalTypography = Typography(
    headlineMedium = style(26, FontWeight.Medium, 1.2f),
    headlineSmall = style(24, FontWeight.Medium, 1.2f),
    titleMedium = style(17, FontWeight.Medium),
    bodyLarge = style(16, FontWeight.Normal, 1.5f),
    bodyMedium = style(15, FontWeight.Normal, 1.45f),
    bodySmall = style(13, FontWeight.Normal),
    labelLarge = style(15, FontWeight.SemiBold),
    labelMedium = style(13, FontWeight.Medium),
    labelSmall = style(12, FontWeight.Medium),
)
