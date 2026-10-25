package com.personal.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes

/** Soft pill chip: filled white when selected, dark surface otherwise. 32dp visual, 48dp touch target. */
@Composable
fun TagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingClear: Boolean = false,
) {
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .height(32.dp)
                .clip(JournalShapes.pill)
                .background(if (selected) JournalColors.PrimaryFill else JournalColors.Surface)
                .padding(start = 14.dp, end = if (trailingClear) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) JournalColors.OnPrimary else JournalColors.TextSecondary,
            )
            if (trailingClear) {
                Icon(JournalIcons.Close, contentDescription = null, tint = JournalColors.OnPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Dashed pill used for "+ Create" actions. */
@Composable
fun DashedChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .selectable(selected = false, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .height(32.dp)
                .drawBehind {
                    drawRoundRect(
                        color = JournalColors.Hairline.copy(alpha = 1f),
                        cornerRadius = CornerRadius(size.height / 2),
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
                    )
                }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = JournalColors.TextMuted)
        }
    }
}

/** Small label used on tiles and photos: "Today, 25", "23", "18:30". */
@Composable
fun DateLabel(text: String, emphasised: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(24.dp)
            .clip(JournalShapes.pill)
            .background(if (emphasised) JournalColors.PrimaryFill else JournalColors.Scrim)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasised) JournalColors.OnPrimary else JournalColors.TextPrimary,
        )
    }
}

/** Read-only tag shown on cards and the entry screen. */
@Composable
fun TagLabel(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(26.dp)
            .clip(JournalShapes.pill)
            .background(JournalColors.SurfaceRaised)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = JournalColors.TextSecondary)
    }
}
