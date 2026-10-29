package com.personal.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import com.personal.journal.ui.theme.tagColour

/**
 * Pill chip: unselected is a bordered card with the tag's colour dot, selected is filled with the tag colour.
 * [tagged] = false (the "All" filter) has no dot and selects to action/onAction. 32dp visual, 48dp touch target.
 */
@Composable
fun TagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingClear: Boolean = false,
    tagged: Boolean = true,
) {
    val colour = tagColour(label)
    val fill = when {
        !selected -> JournalColors.Card
        tagged -> colour.fill
        else -> JournalColors.Action
    }
    val ink = when {
        !selected -> JournalColors.TextSecondary
        tagged -> colour.onFill
        else -> JournalColors.OnAction
    }
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
                .background(fill)
                .then(if (selected) Modifier else Modifier.border(1.5.dp, JournalColors.Line, JournalShapes.pill))
                .padding(start = 14.dp, end = if (trailingClear) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (tagged && !selected) Box(Modifier.size(9.dp).clip(CircleShape).background(colour.fill))
            Text(label, style = MaterialTheme.typography.labelMedium, color = ink)
            if (trailingClear) {
                Icon(JournalIcons.Close, contentDescription = null, tint = ink, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Dashed pill used for "+ Create" actions. */
@Composable
fun DashedChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dashColor = JournalColors.Line
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
                        color = dashColor.copy(alpha = 1f),
                        cornerRadius = CornerRadius(size.height / 2),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
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
            .background(if (emphasised) JournalColors.Action else JournalColors.Paper)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasised) JournalColors.OnAction else JournalColors.OnPaper,
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
            .background(JournalColors.Sunk)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = JournalColors.TextSecondary)
    }
}
