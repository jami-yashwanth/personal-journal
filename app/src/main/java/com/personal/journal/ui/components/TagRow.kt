package com.personal.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.personal.journal.data.normalizeTag
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes

data class TagRowState(
    val tags: List<String> = emptyList(),
    val selected: Set<String> = emptySet(),
    val searching: Boolean = false,
    val query: String = "",
    val results: List<String> = emptyList(),
    val canCreate: Boolean = false,
)

/** "+ Create" is offered when the normalised query is valid and not already an exact result. */
fun canCreateTag(query: String, results: List<String>): Boolean {
    val name = normalizeTag(query) ?: return false
    return name !in results
}

@Composable
fun TagRow(
    state: TagRowState,
    onToggle: (String) -> Unit,
    onQuery: (String) -> Unit,
    onCreate: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.searching) {
        Row(
            modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenSearch) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(JournalColors.Card), contentAlignment = Alignment.Center) {
                    Icon(JournalIcons.Search, contentDescription = "Search tags", tint = JournalColors.TextSecondary, modifier = Modifier.size(15.dp))
                }
            }
            val ordered = state.tags.sortedBy { if (it in state.selected) 0 else 1 }
            ordered.forEach { TagChip(it, selected = it in state.selected, onClick = { onToggle(it) }) }
        }
        return
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth().height(46.dp).clip(JournalShapes.pill).background(JournalColors.Card)
                .padding(start = 16.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(JournalIcons.Search, contentDescription = null, tint = JournalColors.TextMuted, modifier = Modifier.size(16.dp))
            BasicTextField(
                value = state.query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = JournalColors.TextPrimary),
                cursorBrush = SolidColor(JournalColors.TextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (state.canCreate) onCreate(state.query) else state.results.firstOrNull()?.let(onToggle)
                }),
                modifier = Modifier.weight(1f).focusRequester(focus),
                decorationBox = { inner ->
                    if (state.query.isEmpty()) Text("Search or create a tag", style = MaterialTheme.typography.bodyLarge, color = JournalColors.TextMuted)
                    inner()
                },
            )
            IconButton(onClick = onCloseSearch) {
                Icon(JournalIcons.Close, contentDescription = "Close tag search", tint = JournalColors.TextMuted, modifier = Modifier.size(16.dp))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            state.results.forEach { TagChip(it, selected = it in state.selected, onClick = { onToggle(it) }) }
            if (state.canCreate) DashedChip("+ Create \"${state.query.trim()}\"", onClick = { onCreate(state.query) })
        }
        if (state.selected.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Selected", style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
                state.selected.forEach { TagChip(it, selected = true, onClick = { onToggle(it) }) }
            }
        }
    }
}
