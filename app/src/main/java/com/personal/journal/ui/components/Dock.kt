package com.personal.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import com.personal.journal.ui.theme.LocalJournalPalette

private val DangerOnCream = Color(0xFFC2414B)

/** Bottom padding for scrolling content that runs under a [Dock]. */
val DockClearance: Dp = 100.dp

data class DockAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val primary: Boolean = false,
    val danger: Boolean = false,
)

/** Floating action pill: icon slots, with the primary one raised into an onAction circle. */
@Composable
fun Dock(actions: List<DockAction>, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(68.dp)
            .clip(JournalShapes.pill)
            .background(JournalColors.Action)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        actions.forEach { DockSlot(it) }
    }
}

@Composable
private fun DockSlot(action: DockAction) {
    val size = if (action.primary) 56.dp else 52.dp
    val ink = when {
        action.primary -> JournalColors.Action
        // coral is ~2.2:1 on the cream dark-mode dock; this red is ~4.3:1 there
        action.danger -> if (LocalJournalPalette.current.isDark) DangerOnCream else JournalColors.Danger
        else -> JournalColors.OnAction
    }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (action.primary) Modifier.background(JournalColors.OnAction) else Modifier)
            .clickable(role = Role.Button, onClickLabel = action.label, onClick = action.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(action.icon, contentDescription = action.label, tint = ink, modifier = Modifier.size(22.dp))
    }
}
