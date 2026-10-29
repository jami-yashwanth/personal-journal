package com.personal.journal.ui.month

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.personal.journal.bento.Spot
import com.personal.journal.bento.TileSize
import com.personal.journal.bento.scatter
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.DateLabel
import com.personal.journal.ui.components.Dock
import com.personal.journal.ui.components.DockAction
import com.personal.journal.ui.components.DockClearance
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.components.TagChip
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import com.personal.journal.ui.theme.entryColour
import com.personal.journal.ui.theme.slipStyle
import java.io.File
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MonthScreen(
    vm: MonthViewModel,
    photoFile: (String) -> File,
    banner: String?,
    onOpenDay: (String) -> Unit,
    onCamera: () -> Unit,
    onImport: (List<Uri>) -> Unit,
    onWrite: () -> Unit,
    onSettings: () -> Unit,
    onRestore: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val state = s ?: return Box(Modifier.fillMaxSize().background(JournalColors.Background))

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) onImport(uris)
    }
    val launchPicker = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val freshMonth = state.isEmpty && state.isCurrentMonth

    Box(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding()
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total > 120f) vm.previous() else if (total < -120f) vm.next()
                    },
                ) { _, dx -> total += dx }
            },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(state.title, style = MaterialTheme.typography.displayLarge, color = JournalColors.TextPrimary)
                    Text(
                        state.subtitle, style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                CircleIconButton(JournalIcons.Settings, "Settings", onSettings)
            }
            if (banner != null) {
                Text(
                    banner,
                    style = MaterialTheme.typography.bodySmall,
                    color = JournalColors.Danger,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp).clickable(onClick = onSettings),
                )
            }
            if (state.filterTags.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp, start = 12.dp).horizontalScroll(rememberScrollState()),
                ) {
                    TagChip("All", selected = state.activeTag == null, onClick = { vm.setTag(null) }, tagged = false)
                    state.filterTags.forEach { tag ->
                        val on = tag == state.activeTag
                        TagChip(tag, selected = on, trailingClear = on, onClick = { vm.setTag(if (on) null else tag) })
                    }
                }
            }
            if (state.isEmpty) {
                EmptyMonth(
                    isCurrentMonth = state.isCurrentMonth,
                    previousName = state.month.minusMonths(1).month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    onCamera = onCamera,
                    onLookBack = vm::previous,
                    onRestore = onRestore,
                )
            } else {
                BentoGrid(
                    state.tiles.map { it.placed },
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = if (state.filterTags.isEmpty()) 20.dp else 12.dp),
                ) {
                    state.tiles.forEach { tile -> Tile(tile, photoFile, onClick = { onOpenDay(tile.placed.localDate) }) }
                }
            }
            Spacer(Modifier.height(DockClearance))
        }
        if (!freshMonth) {
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
}

@Composable
private fun Tile(tile: TileUi, photoFile: (String) -> File, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = if (tile.dimmed) 0.18f else 1f }.clip(JournalShapes.tile)
            .background(JournalColors.Sunk)
            .clickable(role = Role.Button, onClickLabel = "Open ${tile.label}", onClick = onClick),
    ) {
        Scrapbook(tile, photoFile)
        if (tile.hiddenCount > 0) {
            Text(
                "+${tile.hiddenCount}",
                style = MaterialTheme.typography.labelSmall,
                color = JournalColors.OnAction,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(JournalShapes.pill).background(JournalColors.Action)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        DateLabel(tile.label, emphasised = tile.isToday, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
    }
}

/** The day's photos as small prints and its notes as tag-coloured slips, dropped on the tile the same way every time. */
@Composable
private fun Scrapbook(tile: TileUi, photoFile: (String) -> File) {
    // The bottom stays clear for the date label.
    val bottomClear = 26.dp
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = 7.dp, top = 7.dp, end = 7.dp, bottom = bottomClear)) {
        val density = LocalDensity.current
        val spots = remember(tile.placed.localDate, tile.items.size, maxWidth, maxHeight) {
            scatter(tile.items.size, aspect = maxWidth / maxHeight, seed = tile.placed.localDate.hashCode())
        }
        val alone = tile.items.size == 1
        val noteSize = when {
            alone -> 15.sp
            tile.placed.size == TileSize.LARGE -> 13.sp
            else -> 12.sp
        }
        val style = remember(noteSize) { slipStyle(noteSize) }
        val maxLines = with(density) {
            slipMaxLines((maxHeight + bottomClear / 2).toPx(), style.lineHeight.toPx(), 12.dp.toPx(), alone)
        }
        // Slips go on top of the prints.
        tile.items.zip(spots).sortedBy { (item, _) -> item is TileItem.Note }.forEach { (item, cell) ->
            when (item) {
                is TileItem.Photo -> Box(
                    Modifier.offset(maxWidth * cell.x, maxHeight * cell.y)
                        .size(maxWidth * cell.width, maxHeight * cell.height)
                        .graphicsLayer { rotationZ = cell.rotation }
                        .clip(JournalShapes.print).background(JournalColors.Paper)
                        .padding(start = 3.dp, top = 3.dp, end = 3.dp, bottom = 8.dp),
                ) {
                    AsyncImage(
                        photoFile(item.thumb), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).background(JournalColors.Line),
                    )
                }
                is TileItem.Note -> {
                    // Words need more width than a print; a slip spreads around its spot and may lie across a neighbour.
                    val spot = cell.widened(1.6f, max = 0.92f)
                    val colour = entryColour(item.tags)
                    Text(
                        item.text,
                        style = style,
                        color = colour.onFill,
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            // Wraps its text, centred on its spot and kept inside the tile.
                            .layout { measurable, constraints ->
                                val w = constraints.maxWidth
                                val h = constraints.maxHeight
                                val p = measurable.measure(Constraints(maxWidth = (w * spot.width).roundToInt()))
                                val centre = (spot.y + spot.height / 2) * h
                                val y = (centre - p.height / 2f).coerceIn(0f, maxOf(0, h - p.height).toFloat())
                                layout(w, h) { p.place((w * spot.x).roundToInt(), y.roundToInt()) }
                            }
                            .graphicsLayer { rotationZ = spot.rotation }
                            .clip(JournalShapes.print).background(colour.fill)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

private fun Spot.widened(factor: Float, max: Float): Spot {
    val w = (width * factor).coerceAtMost(max).coerceAtLeast(width)
    return copy(x = (x + width / 2 - w / 2).coerceIn(0f, 1 - w), width = w)
}

@Composable
private fun EmptyMonth(
    isCurrentMonth: Boolean,
    previousName: String,
    onCamera: () -> Unit,
    onLookBack: () -> Unit,
    onRestore: () -> Unit,
) {
    if (!isCurrentMonth) {
        Text(
            "No entries this month.",
            style = MaterialTheme.typography.bodyMedium,
            color = JournalColors.TextMuted,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
        )
        return
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 210.dp, height = 140.dp)) {
            BlankTile(Color(0xFF2E8A78), Modifier.offset(8.dp, 20.dp).graphicsLayer { rotationZ = -10f })
            BlankTile(Color(0xFFF5B840), Modifier.offset(64.dp, 0.dp)) {
                Text("1", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = JournalColors.OnColour)
            }
            BlankTile(Color(0xFFF07F86), Modifier.offset(120.dp, 24.dp).graphicsLayer { rotationZ = 9f })
        }
        Text(
            "A fresh month.",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = JournalColors.TextPrimary,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            "Every day you capture becomes a tile here. Busier days get bigger.",
            style = MaterialTheme.typography.bodyLarge,
            color = JournalColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp).width(270.dp),
        )
        PrimaryButton("Take the first photo", onCamera, Modifier.padding(top = 28.dp), arrow = true)
        Text(
            "look back at $previousName",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline),
            color = JournalColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp).clickable(role = Role.Button, onClick = onLookBack).padding(horizontal = 12.dp, vertical = 12.dp),
        )
        Text(
            "Restore from backup",
            style = MaterialTheme.typography.bodySmall,
            color = JournalColors.TextMuted,
            modifier = Modifier.clickable(role = Role.Button, onClick = onRestore).padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun BlankTile(colour: Color, modifier: Modifier, content: @Composable () -> Unit = {}) {
    Box(modifier.size(82.dp, 108.dp).clip(JournalShapes.tile).background(colour), contentAlignment = Alignment.Center) { content() }
}
