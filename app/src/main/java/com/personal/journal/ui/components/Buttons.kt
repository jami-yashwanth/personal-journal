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

/** White filled button (the single primary action on a screen). */
@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, height: Dp = 52.dp) {
    Box(
        modifier
            .height(height)
            .clip(JournalShapes.button)
            .background(JournalColors.PrimaryFill)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = JournalColors.OnPrimary)
    }
}

/** Soft surface button, optionally with a leading icon. */
@Composable
fun SoftButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 50.dp) {
    Row(
        modifier
            .height(height)
            .clip(JournalShapes.button)
            .background(JournalColors.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = JournalColors.TextPrimary, modifier = Modifier.size(17.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = JournalColors.TextPrimary)
    }
}

/** Round icon button on a soft or translucent background. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = JournalColors.Surface,
    tint: Color = JournalColors.TextPrimary,
) {
    Box(
        modifier
            .size(maxOf(size, 48.dp))
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
        }
    }
}

/** Isolated destructive button (outlined, muted red). */
@Composable
fun DestructiveIconButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(50.dp)
            .clip(JournalShapes.button)
            .border(1.dp, JournalColors.DestructiveOutline, JournalShapes.button)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(JournalIcons.Trash, contentDescription = contentDescription, tint = JournalColors.DestructiveIcon, modifier = Modifier.size(19.dp))
    }
}
