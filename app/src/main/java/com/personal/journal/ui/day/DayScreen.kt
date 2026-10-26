package com.personal.journal.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.personal.journal.data.db.EntryType
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import com.personal.journal.ui.theme.handwriting
import com.personal.journal.ui.timeLabel
import java.io.File

/** A photo whose size can't be read is shown at this shape rather than not at all. */
private const val FALLBACK_ASPECT = 4f / 3f

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
        // Two columns, each moment in time order dropping into whichever column is shorter.
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(Modifier.padding(top = 12.dp)) {
                    CircleIconButton(JournalIcons.Back, "Back to month", onBack)
                    Column(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        Text(state.title, style = MaterialTheme.typography.headlineMedium, color = JournalColors.TextPrimary)
                        Text(state.subtitle, style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
                    }
                }
            }
            // A lone moment gets the full width rather than half a page with an empty column beside it.
            val span = if (state.items.size == 1) StaggeredGridItemSpan.FullLine else null
            items(state.items, key = { it.entry.entry.id }, span = span?.let { full -> { full } }) { item ->
                val entry = item.entry.entry
                val open = { onOpenEntry(entry.id) }
                if (entry.type == EntryType.PHOTO && entry.photoFile != null) {
                    PhotoTile(photoFile(entry.photoFile), item.aspect ?: FALLBACK_ASPECT, timeLabel(entry.takenAt), entry.text, open)
                } else {
                    NoteTile(entry.text.orEmpty(), timeLabel(entry.takenAt), open)
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(96.dp)) }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp).clip(RoundedCornerShape(18.dp))
                .background(JournalColors.PrimaryFill).clickable(role = Role.Button, onClickLabel = "Capture", onClick = onCapture),
            contentAlignment = Alignment.Center,
        ) { Icon(JournalIcons.Camera, contentDescription = "Capture", tint = JournalColors.OnPrimary, modifier = Modifier.size(22.dp)) }
    }
}

/** The photo at its own shape with its time on it, and the caption underneath in the user's handwriting. */
@Composable
private fun PhotoTile(file: File, aspect: Float, time: String, caption: String?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(JournalShapes.button).background(JournalColors.Surface)
            .clickable(role = Role.Button, onClickLabel = "Open photo from $time", onClick = onClick),
    ) {
        Box {
            AsyncImage(
                file, contentDescription = caption ?: "Photo", contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(aspect).background(JournalColors.SurfaceRaised),
            )
            Text(
                time,
                style = MaterialTheme.typography.labelSmall,
                color = JournalColors.TextPrimary,
                modifier = Modifier.align(Alignment.BottomStart).padding(7.dp).clip(JournalShapes.pill)
                    .background(JournalColors.Scrim).padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
        if (!caption.isNullOrBlank()) {
            Text(
                caption,
                style = handwriting(17.sp),
                color = JournalColors.TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

/** A written entry: its time, then the words, growing with the note up to about eight lines. */
@Composable
private fun NoteTile(text: String, time: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(JournalShapes.button).background(JournalColors.Surface)
            .clickable(role = Role.Button, onClickLabel = "Open note from $time", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(time, style = MaterialTheme.typography.labelSmall, color = JournalColors.TextMuted)
        Text(text, style = handwriting(18.sp), color = JournalColors.TextSecondary, maxLines = 8, overflow = TextOverflow.Ellipsis)
    }
}
