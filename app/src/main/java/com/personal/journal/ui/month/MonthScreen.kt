package com.personal.journal.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.personal.journal.bento.TileSize
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.DateLabel
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.components.TagChip
import com.personal.journal.ui.theme.Caveat
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import java.io.File
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.personal.journal.bento.Spot
import com.personal.journal.bento.scatter
import androidx.compose.runtime.remember

@Composable
fun MonthScreen(
    vm: MonthViewModel,
    photoFile: (String) -> File,
    banner: String?,
    onOpenDay: (String) -> Unit,
    onCapture: () -> Unit,
    onWrite: () -> Unit,
    onSettings: () -> Unit,
    onRestore: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val state = s ?: return Box(Modifier.fillMaxSize().background(JournalColors.Background))

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
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(state.title, style = MaterialTheme.typography.headlineMedium, color = JournalColors.TextPrimary)
                    Text(state.subtitle, style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
                }
                CircleIconButton(JournalIcons.Settings, "Settings", onSettings, tint = JournalColors.TextSecondary)
            }
            if (banner != null) {
                Text(
                    banner,
                    style = MaterialTheme.typography.bodySmall,
                    color = JournalColors.DestructiveIcon,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp).clickable(onClick = onSettings),
                )
            }
            if (state.filterTags.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, start = 12.dp).horizontalScroll(rememberScrollState()),
                ) {
                    TagChip("All", selected = state.activeTag == null, onClick = { vm.setTag(null) })
                    state.filterTags.forEach { tag ->
                        val on = tag == state.activeTag
                        TagChip(tag, selected = on, trailingClear = on, onClick = { vm.setTag(if (on) null else tag) })
                    }
                }
            }
            if (state.isEmpty) {
                EmptyMonth(state.isCurrentMonth, onCapture, onWrite, onRestore)
            } else {
                BentoGrid(
                    state.tiles.map { it.placed },
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = if (state.filterTags.isEmpty()) 20.dp else 8.dp),
                ) {
                    state.tiles.forEach { tile -> Tile(tile, photoFile, onClick = { onOpenDay(tile.placed.localDate) }) }
                }
            }
            Spacer(Modifier.height(100.dp))
        }
        if (!(state.isEmpty && state.isCurrentMonth)) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp).clip(RoundedFab).background(JournalColors.PrimaryFill)
                    .clickable(role = Role.Button, onClickLabel = "Capture", onClick = onCapture),
                contentAlignment = Alignment.Center,
            ) { Icon(JournalIcons.Camera, contentDescription = "Capture", tint = JournalColors.OnPrimary, modifier = Modifier.size(22.dp)) }
        }
    }
}

private val RoundedFab = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)

@Composable
private fun Tile(tile: TileUi, photoFile: (String) -> File, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = if (tile.dimmed) 0.18f else 1f }.clip(JournalShapes.tile)
            .background(JournalColors.Surface)
            .clickable(role = Role.Button, onClickLabel = "Open ${tile.label}", onClick = onClick),
    ) {
        Scrapbook(tile, photoFile)
        if (tile.hiddenCount > 0) {
            Text(
                "+${tile.hiddenCount}",
                style = MaterialTheme.typography.labelSmall,
                color = JournalColors.TextPrimary,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(JournalShapes.pill).background(JournalColors.Scrim)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
        DateLabel(tile.label, emphasised = tile.isToday, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
    }
}

/** The day's photos as small prints and its notes as loose lines, dropped on the tile the same way every time. */
@Composable
private fun Scrapbook(tile: TileUi, photoFile: (String) -> File) {
    // The bottom stays clear for the date label.
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = 7.dp, top = 7.dp, end = 7.dp, bottom = 26.dp)) {
        val spots = remember(tile.placed.localDate, tile.items.size, maxWidth, maxHeight) {
            scatter(tile.items.size, aspect = maxWidth / maxHeight, seed = tile.placed.localDate.hashCode())
        }
        // Handwriting runs small, so notes are set larger than body text.
        val noteSize = when {
            tile.items.size == 1 -> 18.sp
            tile.placed.size == TileSize.LARGE -> 16.sp
            else -> 14.sp
        }
        // Notes go on top of the prints, as loose lines with no backing.
        tile.items.zip(spots).sortedBy { (item, _) -> item is TileItem.Note }.forEach { (item, cell) ->
            // Words need more width than a print; a note spreads around its spot and may lie across a neighbour.
            val spot = if (item is TileItem.Note) cell.widened(1.6f, max = 0.92f) else cell
            val placed = Modifier.offset(maxWidth * spot.x, maxHeight * spot.y)
                .size(maxWidth * spot.width, maxHeight * spot.height)
                .graphicsLayer { rotationZ = spot.rotation }
            when (item) {
                is TileItem.Photo -> Box(
                    placed.clip(PrintShape).background(JournalColors.Paper).padding(start = 3.dp, top = 3.dp, end = 3.dp, bottom = 8.dp),
                ) {
                    AsyncImage(
                        photoFile(item.thumb), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(JournalColors.SurfaceRaised),
                    )
                }
                is TileItem.Note -> Box(placed, contentAlignment = Alignment.CenterStart) {
                    Text(
                        item.text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Caveat,
                            fontWeight = FontWeight.Medium,
                            fontSize = noteSize,
                            lineHeight = noteSize * 1.1f,
                        ),
                        color = JournalColors.TextSecondary,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
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

private val PrintShape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)

@Composable
private fun EmptyMonth(isCurrentMonth: Boolean, onCapture: () -> Unit, onWrite: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp).padding(top = 22.dp)) {
        if (isCurrentMonth) {
            Column(
                Modifier.fillMaxWidth().height(228.dp).clip(JournalShapes.card).background(JournalColors.Surface).padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                DateLabel("Today, ${java.time.LocalDate.now().dayOfMonth}", emphasised = true)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Your first entry goes here. Take a photo, or write a line about today.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = JournalColors.TextPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton("Take a photo", onCapture, height = 46.dp)
                        Box(
                            Modifier.height(46.dp).clip(JournalShapes.button).background(JournalColors.SurfaceRaised)
                                .clickable(role = Role.Button, onClick = onWrite).padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Write", style = MaterialTheme.typography.labelLarge.copy(fontWeight = null), color = JournalColors.TextPrimary) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostTile(Modifier.weight(1f))
                GhostTile(Modifier.weight(2f))
            }
            Text(
                "Each day you log becomes a tile. Busier days get bigger ones.",
                style = MaterialTheme.typography.bodySmall,
                color = JournalColors.TextMuted,
                modifier = Modifier.padding(top = 18.dp, start = 10.dp, end = 10.dp),
            )
            Text(
                "Restore from backup",
                style = MaterialTheme.typography.bodySmall,
                color = JournalColors.TextSecondary,
                modifier = Modifier.padding(top = 14.dp, start = 10.dp).clickable(role = Role.Button, onClick = onRestore).padding(vertical = 12.dp),
            )
        } else {
            Text(
                "No entries this month.",
                style = MaterialTheme.typography.bodyMedium,
                color = JournalColors.TextMuted,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
private fun GhostTile(modifier: Modifier) {
    Box(
        modifier.height(110.dp).drawBehind {
            drawRoundRect(
                color = JournalColors.Hairline,
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))),
            )
        },
    )
}
