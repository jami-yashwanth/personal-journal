package com.personal.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes

private val BorderWidth = 1.5.dp
private val OnColourInk = Color(0xFF1E1B19)

/** Filled action pill (the single primary action on a screen). With [arrow], the label sits at the start and a circle holding an arrow at the end. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    arrow: Boolean = false,
    onColour: Boolean = false,
) {
    val fill = if (onColour) OnColourInk else JournalColors.Action
    val ink = if (onColour) Color.White else JournalColors.OnAction
    Row(
        modifier
            .height(height)
            .clip(JournalShapes.pill)
            .background(fill)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = 26.dp, end = if (arrow) 10.dp else 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (arrow) Arrangement.SpaceBetween else Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = ink, modifier = if (arrow) Modifier.padding(end = 14.dp) else Modifier)
        if (arrow) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(ink), contentAlignment = Alignment.Center) {
                Icon(JournalIcons.Arrow, contentDescription = null, tint = fill, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Card pill with a line border, optionally with a leading icon. */
@Composable
fun SoftButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 50.dp) {
    Row(
        modifier
            .height(height)
            .clip(JournalShapes.pill)
            .background(JournalColors.Card)
            .border(BorderWidth, JournalColors.Line, JournalShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = JournalColors.TextPrimary, modifier = Modifier.size(17.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = JournalColors.TextPrimary)
    }
}

/** Round icon button: a bordered card circle, or a paper circle when it sits on a tag colour. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    background: Color = JournalColors.Card,
    tint: Color = JournalColors.TextPrimary,
    onColour: Boolean = false,
) {
    val fill = if (onColour) JournalColors.Paper else background
    val ink = if (onColour) JournalColors.OnPaper else tint
    Box(
        modifier
            .size(maxOf(size, 48.dp))
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(fill)
                .then(if (onColour) Modifier else Modifier.border(BorderWidth, JournalColors.Line, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = ink, modifier = Modifier.size(19.dp))
        }
    }
}

/** Isolated destructive button (outlined, muted red). */
@Composable
fun DestructiveIconButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(50.dp)
            .clip(JournalShapes.pill)
            .border(BorderWidth, JournalColors.Line, JournalShapes.pill)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(JournalIcons.Trash, contentDescription = contentDescription, tint = JournalColors.Danger, modifier = Modifier.size(19.dp))
    }
}
