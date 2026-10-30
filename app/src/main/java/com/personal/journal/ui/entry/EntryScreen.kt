package com.personal.journal.ui.entry

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import com.personal.journal.ui.motion.ImageKeys
import com.personal.journal.ui.motion.noteKey
import com.personal.journal.ui.motion.photoKey
import com.personal.journal.ui.motion.sharedPhoto
import com.personal.journal.ui.motion.sharedTile
import com.personal.journal.data.db.EntryType
import com.personal.journal.data.db.EntryWithTags
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.DateTimePickerDialog
import com.personal.journal.ui.components.Dock
import com.personal.journal.ui.components.DockAction
import com.personal.journal.ui.components.DockClearance
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.components.SoftButton
import com.personal.journal.ui.components.SoftTextField
import com.personal.journal.ui.components.TagRow
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import com.personal.journal.ui.theme.LocalJournalPalette
import com.personal.journal.ui.theme.TagColour
import com.personal.journal.ui.theme.entryColour
import com.personal.journal.ui.dayTitle
import com.personal.journal.ui.timeLabel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.min

private const val HERO_ASPECT = 300f / 430f

@Composable
fun EntryScreen(vm: EntryViewModel, photoFile: (String) -> File, tintCache: TintCache, imageKeys: ImageKeys, onBack: () -> Unit) {
    val day by vm.day.collectAsStateWithLifecycle()
    val e by vm.entry.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val dayIsEmpty by vm.dayIsEmpty.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(dayIsEmpty) { if (dayIsEmpty) onBack() }
    val current = e ?: return Box(Modifier.fillMaxSize().background(JournalColors.Background))
    val entry = current.entry
    val scope = rememberCoroutineScope()
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var draft by remember(entry.id, vm.editTags) { mutableStateOf(entry.text.orEmpty()) }
    val editing = vm.editTags

    // selected, not initialIndex: a fresh composition (rotation, theme switch) lands where the user left off
    val pager = rememberPagerState(initialPage = selected) { day.size }
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect(vm::select) }
    // Delete and date changes move the selection; bring the pager along.
    LaunchedEffect(selected) { if (pager.settledPage != selected && !pager.isScrollInProgress) pager.scrollToPage(selected) }

    // The page under the finger drives the background, so it cross-fades mid-swipe.
    val shown = day.getOrNull(pager.currentPage) ?: current
    val shownColour = shown.colourOrNull()
    val dark = LocalJournalPalette.current.isDark
    val tint by produceState<Color?>(null, shown.entry.id, shown.entry.thumbFile, dark) {
        value = null
        value = tintCache.tint(shown.entry.id, shown.entry.thumbFile?.let(photoFile), dark)
    }
    val background by animateColorAsState(shownColour?.fill ?: tint ?: JournalColors.Background, label = "entry background")
    // 0.25 sits just above the point where dark and white icons contrast equally, so mid pastels get dark icons
    StatusBarIcons(light = background.luminance() > 0.25f)
    val headerInk = shownColour?.onFill ?: JournalColors.TextSecondary

    Box(Modifier.fillMaxSize().background(background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(JournalIcons.Back, "Back", onBack, onColour = shownColour != null)
                Text(
                    "${pager.settledPage + 1} of ${day.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = headerInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(48.dp))
            }
            if (editing == null) {
                HorizontalPager(
                    pager,
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 45.dp),
                    pageSpacing = 25.dp,
                    key = { day[it].entry.id },
                ) { page ->
                    val item = day[page]
                    val colour = item.colourOrNull()
                    // text belongs to the settled page; in a side peek only the card shows
                    // a lambda, so the per-frame offset is read in the graphicsLayer and does not recompose every page
                    val textAlpha = { 1f - min(1f, abs(pager.currentPage - page + pager.currentPageOffsetFraction)) }
                    val shared = page == pager.settledPage
                    if (colour == null) PhotoPage(item, photoFile, imageKeys, shared, textAlpha) else TextPage(item, colour, shared, textAlpha)
                }
            } else {
                val tagState by editing.state.collectAsStateWithLifecycle()
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SoftTextField(draft, { draft = it }, if (entry.type == EntryType.TEXT) "Write something" else "Add a caption", Modifier.fillMaxWidth())
                    TagRow(
                        tagState, onToggle = editing::toggle, onQuery = editing::query, onCreate = editing::create,
                        onOpenSearch = editing::openSearch, onCloseSearch = editing::closeSearch,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SoftButton("Cancel", onClick = vm::cancelEdit)
                    Spacer(Modifier.weight(1f))
                    val canSave = entry.type == EntryType.PHOTO || draft.isNotBlank()
                    PrimaryButton("Save", enabled = canSave, onClick = { scope.launch { vm.saveEdit(draft) } })
                }
            }
        }
        if (editing == null) {
            Dock(
                listOf(
                    DockAction(JournalIcons.Pen, "Edit", vm::startEdit),
                    DockAction(JournalIcons.Calendar, "Change date", { pickDate = true }),
                    DockAction(JournalIcons.Trash, "Delete", { confirmDelete = true }, danger = true),
                ),
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
            )
        }
    }

    if (pickDate) {
        DateTimePickerDialog(entry.takenAt, onPicked = { pickDate = false; scope.launch { vm.updateDate(it) } }, onDismiss = { pickDate = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = JournalColors.Card,
            title = { Text("Delete this entry?", color = JournalColors.TextPrimary) },
            text = {
                Text(
                    "The photo and caption will be removed from this phone and from your backup.",
                    color = JournalColors.TextSecondary,
                )
            },
            confirmButton = {
                // the last entry of the day empties it, and dayIsEmpty takes the screen back
                TextButton(onClick = { confirmDelete = false; scope.launch { vm.delete() } }) {
                    Text("Delete", color = JournalColors.TextPrimary)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = JournalColors.TextPrimary) } },
        )
    }
}

/** A text entry's tag colour; null for a photo, whose page takes the photo tint instead. */
private fun EntryWithTags.colourOrNull(): TagColour? =
    if (entry.type == EntryType.TEXT) entryColour(tags.map { it.name }) else null

/** Day and time, the photo as a 300×430 hero, and the caption clamped to three lines until tapped. */
@Composable
private fun PhotoPage(item: EntryWithTags, photoFile: (String) -> File, imageKeys: ImageKeys, shared: Boolean, textAlpha: () -> Float) {
    val entry = item.entry
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    var overflows by remember(entry.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = DockClearance),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            dayTitle(entry.localDate), style = MaterialTheme.typography.displayLarge, color = JournalColors.TextPrimary, maxLines = 1,
            modifier = Modifier.graphicsLayer { alpha = textAlpha() },
        )
        Text(
            timeLabel(entry.takenAt), style = MaterialTheme.typography.bodySmall, color = JournalColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = textAlpha() },
        )
        Box(
            Modifier
                .padding(top = 18.dp)
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .aspectRatio(HERO_ASPECT)
                .then(if (shared) Modifier.sharedPhoto(photoKey(entry.id), JournalShapes.hero) else Modifier)
                .clip(JournalShapes.hero)
                .background(JournalColors.Sunk),
        ) {
            entry.photoFile?.let {
                // The Day tile's decoded photo stands in while the full size loads, so nothing grey flies in.
                // Decoded at the hero's full size: it may first be laid out mid-flight, at a tile's size, and Coil would keep that.
                val full = with(LocalDensity.current) { Size(300.dp.roundToPx(), (300f / HERO_ASPECT).dp.roundToPx()) }
                val request = ImageRequest.Builder(LocalContext.current).data(photoFile(it)).size(full)
                    .placeholderMemoryCacheKey(imageKeys.get(entry.id)).build()
                AsyncImage(
                    request, contentDescription = entry.text ?: "Photo",
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
            }
        }
        entry.text?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleLarge,
                color = JournalColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                // only a caption cut short by the clamp is worth tapping
                onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
                modifier = Modifier
                    .graphicsLayer { alpha = textAlpha() }
                    .padding(top = 24.dp, start = 4.dp, end = 4.dp)
                    .then(
                        if (expanded || overflows) {
                            Modifier.clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse caption" else "Show full caption") { expanded = !expanded }
                        } else Modifier,
                    ),
            )
        }
    }
}

/** A text entry as a big index card in its tag colour; long text scrolls inside it. */
@Composable
private fun TextPage(item: EntryWithTags, colour: TagColour, shared: Boolean, textAlpha: () -> Float) {
    val entry = item.entry
    Column(
        Modifier
            .fillMaxSize()
            .then(if (shared) Modifier.sharedTile(noteKey(entry.id), JournalShapes.hero) else Modifier)
            .clip(JournalShapes.hero)
            .background(colour.fill)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = DockClearance),
    ) {
        Row(
            Modifier.graphicsLayer { alpha = textAlpha() }.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item.tags.forEach { TagPill(it.name) }
            Text(
                "${dayTitle(entry.localDate)} · ${timeLabel(entry.takenAt)}",
                style = MaterialTheme.typography.labelLarge, color = colour.onFill, maxLines = 1,
            )
        }
        Text(
            entry.text.orEmpty(),
            style = MaterialTheme.typography.headlineLarge,
            color = colour.onFill,
            modifier = Modifier.padding(top = 22.dp).graphicsLayer { alpha = textAlpha() },
        )
    }
}

/** Dark status and navigation icons on a light page, light ones on a dark page; the theme's own are restored on leaving. */
@Composable
private fun StatusBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as Activity).window
    val controller = remember(window, view) { WindowCompat.getInsetsController(window, view) }
    val original = remember(controller) { controller.isAppearanceLightStatusBars to controller.isAppearanceLightNavigationBars }
    DisposableEffect(light) {
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
        onDispose { }
    }
    DisposableEffect(controller) {
        onDispose {
            controller.isAppearanceLightStatusBars = original.first
            controller.isAppearanceLightNavigationBars = original.second
        }
    }
}

/** Ink pill for a tag sitting on a tag colour. */
@Composable
private fun TagPill(name: String) {
    Box(
        Modifier.height(30.dp).clip(JournalShapes.pill).background(JournalColors.OnColour).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}
