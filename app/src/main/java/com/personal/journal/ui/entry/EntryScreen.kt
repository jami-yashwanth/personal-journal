package com.personal.journal.ui.entry

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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
import com.personal.journal.ui.timeLabel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val HERO_ASPECT = 300f / 430f

@Composable
fun EntryScreen(vm: EntryViewModel, photoFile: (String) -> File, tintCache: TintCache, onBack: () -> Unit) {
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
                    if (colour == null) PhotoPage(item, photoFile) else TextPage(item, colour)
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
                    Text("Delete", color = JournalColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = JournalColors.TextPrimary) } },
        )
    }
}

/** A text entry's tag colour; null for a photo, whose page takes the photo tint instead. */
private fun EntryWithTags.colourOrNull(): TagColour? =
    if (entry.type == EntryType.TEXT) entryColour(tags.map { it.name }) else null

/** "Monday, 26", as on the Day screen. */
private fun dayTitle(localDate: String): String {
    val date = LocalDate.parse(localDate)
    return "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, ${date.dayOfMonth}"
}

/** Day and time, the photo as a 300×430 hero, and the caption clamped to three lines until tapped. */
@Composable
private fun PhotoPage(item: EntryWithTags, photoFile: (String) -> File) {
    val entry = item.entry
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    var overflows by remember(entry.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = DockClearance),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(dayTitle(entry.localDate), style = MaterialTheme.typography.displayLarge, color = JournalColors.TextPrimary, maxLines = 1)
        Text(
            timeLabel(entry.takenAt), style = MaterialTheme.typography.bodySmall, color = JournalColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
        Box(
            Modifier
                .padding(top = 18.dp)
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .aspectRatio(HERO_ASPECT)
                .clip(JournalShapes.hero)
                .background(JournalColors.Sunk),
        ) {
            entry.photoFile?.let {
                AsyncImage(
                    photoFile(it), contentDescription = entry.text ?: "Photo",
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
private fun TextPage(item: EntryWithTags, colour: TagColour) {
    val entry = item.entry
    Column(
        Modifier
            .fillMaxSize()
            .clip(JournalShapes.hero)
            .background(colour.fill)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = DockClearance),
    ) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
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
            modifier = Modifier.padding(top = 22.dp),
        )
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
