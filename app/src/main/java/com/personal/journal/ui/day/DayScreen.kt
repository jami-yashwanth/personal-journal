package com.personal.journal.ui.day

import androidx.compose.ui.unit.sp
import com.personal.journal.ui.theme.handwriting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.personal.journal.data.db.EntryType
import com.personal.journal.data.db.EntryWithTags
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.TagLabel
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import com.personal.journal.ui.timeLabel
import java.io.File

@Composable
fun DayScreen(
    vm: DayViewModel,
    photoFile: (String) -> File,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onCapture: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val state = s ?: return Box(Modifier.fillMaxSize().background(JournalColors.Background))
    Box(Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding()) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Column(Modifier.padding(start = 6.dp, top = 12.dp)) {
                    CircleIconButton(JournalIcons.Back, "Back to month", onBack)
                    Column(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 12.dp)) {
                        Text(state.title, style = MaterialTheme.typography.headlineMedium, color = JournalColors.TextPrimary)
                        Text(state.subtitle, style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
                    }
                }
            }
            items(state.entries, key = { it.entry.id }) { e ->
                EntryCard(e, photoFile, Modifier.padding(horizontal = 12.dp)) { onOpenEntry(e.entry.id) }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp).clip(RoundedCornerShape(18.dp))
                .background(JournalColors.PrimaryFill).clickable(role = Role.Button, onClickLabel = "Capture", onClick = onCapture),
            contentAlignment = Alignment.Center,
        ) { Icon(JournalIcons.Camera, contentDescription = "Capture", tint = JournalColors.OnPrimary, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun EntryCard(e: EntryWithTags, photoFile: (String) -> File, modifier: Modifier, onClick: () -> Unit) {
    val entry = e.entry
    Column(
        modifier.fillMaxWidth().clip(JournalShapes.card).background(JournalColors.Surface)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (entry.type == EntryType.PHOTO && entry.photoFile != null) {
            AsyncImage(
                photoFile(entry.photoFile), contentDescription = entry.text ?: "Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).background(JournalColors.SurfaceRaised),
            )
        }
        val hasBody = entry.text != null || e.tags.isNotEmpty() || entry.type == EntryType.TEXT
        if (hasBody) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    entry.text?.let {
                        val isText = entry.type == EntryType.TEXT
                        Text(
                            it,
                            style = handwriting(19.sp),
                            color = if (isText) JournalColors.TextSecondary.copy(alpha = 1f) else JournalColors.TextPrimary,
                            maxLines = if (isText) 3 else Int.MAX_VALUE,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isText && it.length > 140) {
                            Text("Read all", style = MaterialTheme.typography.labelMedium.copy(textDecoration = TextDecoration.Underline), color = JournalColors.TextPrimary)
                        }
                    }
                    if (e.tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { e.tags.forEach { TagLabel(it.name) } }
                    }
                }
                Text(timeLabel(entry.takenAt), style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
            }
        } else {
            Text(
                timeLabel(entry.takenAt),
                style = MaterialTheme.typography.bodySmall,
                color = JournalColors.TextMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}
