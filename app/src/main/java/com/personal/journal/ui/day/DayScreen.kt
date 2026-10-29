package com.personal.journal.ui.day

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import com.personal.journal.data.db.EntryType
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.DateLabel
import com.personal.journal.ui.components.Dock
import com.personal.journal.ui.components.DockAction
import com.personal.journal.ui.components.DockClearance
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.rememberImportPicker
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import com.personal.journal.ui.theme.entryColour
import com.personal.journal.ui.timeLabel
import com.personal.journal.ui.motion.ImageKeys
import com.personal.journal.ui.motion.besideSharedFlight
import com.personal.journal.ui.motion.noteKey
import com.personal.journal.ui.motion.photoKey
import com.personal.journal.ui.motion.rememberTapHaptic
import com.personal.journal.ui.motion.sharedPhoto
import com.personal.journal.ui.motion.sharedTile
import java.io.File

/** A photo whose size can't be read is shown at this shape rather than not at all. */
private const val FALLBACK_ASPECT = 4f / 3f

@Composable
fun DayScreen(
    vm: DayViewModel,
    photoFile: (String) -> File,
    imageKeys: ImageKeys,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onCamera: () -> Unit,
    onImport: (List<Uri>) -> Unit,
    onWrite: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val state = s ?: return Box(Modifier.fillMaxSize().background(JournalColors.Background))
    // Deleting a day's last entry leaves nothing to show; the state only exists once loaded, so this never fires on first load.
    val isEmpty = state.items.isEmpty()
    LaunchedEffect(isEmpty) { if (isEmpty) onBack() }
    val launchPicker = rememberImportPicker(onImport)
    val haptic = rememberTapHaptic()
    Box(Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding()) {
        // Two columns, each moment in time order dropping into whichever column is shorter.
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = DockClearance),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(Modifier.padding(top = 12.dp)) {
                    CircleIconButton(JournalIcons.Back, "Back to month", onBack)
                    Column(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        Text(state.title, style = MaterialTheme.typography.displayLarge, color = JournalColors.TextPrimary)
                        Text(state.subtitle, style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
                    }
                }
            }
            // A lone moment gets the full width rather than half a page with an empty column beside it.
            val span = if (state.items.size == 1) StaggeredGridItemSpan.FullLine else null
            items(state.items, key = { it.entry.entry.id }, span = span?.let { full -> { full } }) { item ->
                val entry = item.entry.entry
                val open = { haptic(); onOpenEntry(entry.id) }
                if (entry.type == EntryType.PHOTO && entry.photoFile != null) {
                    PhotoTile(entry.id, photoFile(entry.photoFile), item.aspect ?: FALLBACK_ASPECT, timeLabel(entry.takenAt), entry.text, imageKeys, open)
                } else {
                    NoteTile(entry.id, entry.text.orEmpty(), timeLabel(entry.takenAt), item.entry.tags.map { it.name }, open)
                }
            }
        }
        Dock(
            listOf(
                DockAction(JournalIcons.Gallery, "Import photos", launchPicker),
                DockAction(JournalIcons.Camera, "Take a photo", onCamera, primary = true),
                DockAction(JournalIcons.Pen, "Write", onWrite),
            ),
            Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
    }
}

/** The photo at its own shape with its time on a paper sticker, and the caption underneath. */
@Composable
private fun PhotoTile(id: String, file: File, aspect: Float, time: String, caption: String?, imageKeys: ImageKeys, onClick: () -> Unit) {
    // The photo takes the card's top corners, and its bottom ones too when there is no caption under it.
    val shape = if (caption.isNullOrBlank()) JournalShapes.card else JournalShapes.card.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)
    Column(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open photo from $time", onClick = onClick)
            .clip(JournalShapes.card).background(JournalColors.Card),
    ) {
        Box {
            // Decoded at a full column's width: the day may first be laid out inside a tile that is still growing.
            val column = LocalWindowInfo.current.containerSize.width / 2
            val request = ImageRequest.Builder(LocalContext.current).data(file).size(Size(column, Dimension.Undefined)).build()
            AsyncImage(
                request, contentDescription = caption ?: "Photo", contentScale = ContentScale.Crop,
                // Clipped here as well as by the card: in flight the photo is drawn on its own, without the card around it.
                modifier = Modifier.fillMaxWidth().aspectRatio(aspect).sharedPhoto(photoKey(id), shape).clip(shape).background(JournalColors.Sunk),
                onSuccess = { s -> s.result.memoryCacheKey?.let { imageKeys.put(id, it) } },
            )
            DateLabel(time, emphasised = false, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).besideSharedFlight())
        }
        if (!caption.isNullOrBlank()) {
            Text(
                caption,
                style = MaterialTheme.typography.bodyLarge,
                color = JournalColors.TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** A written entry on its tag colour: the words, growing to its full length, then "time · tag". */
@Composable
private fun NoteTile(id: String, text: String, time: String, tags: List<String>, onClick: () -> Unit) {
    val colour = entryColour(tags)
    val footer = tags.minOrNull()?.let { "$time · $it" } ?: time
    Column(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open note from $time", onClick = onClick)
            .sharedTile(noteKey(id), JournalShapes.card).clip(JournalShapes.card).background(colour.fill)
            .padding(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colour.onFill)
        Text(footer, style = MaterialTheme.typography.bodySmall, color = colour.onFill, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
