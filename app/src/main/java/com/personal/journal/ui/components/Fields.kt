package com.personal.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes

/** Single-line soft field used for captions. */
@Composable
fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 52.dp,
    shape: Shape = JournalShapes.button,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    background: androidx.compose.ui.graphics.Color = JournalColors.Card,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style.copy(color = JournalColors.TextPrimary),
        cursorBrush = SolidColor(JournalColors.TextPrimary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape).background(background).padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(placeholder, style = style, color = JournalColors.TextMuted)
                inner()
            }
        },
    )
}

/** Error copy in TextPrimary (Danger fails contrast as text) led by a small Danger dot. */
@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        Box(
            Modifier.padding(top = 5.dp, end = 8.dp).size(8.dp).clip(CircleShape)
                .background(JournalColors.Danger),
        )
        Text(message, style = style, color = JournalColors.TextPrimary)
    }
}
