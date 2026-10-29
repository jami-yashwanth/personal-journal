package com.personal.journal.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.personal.journal.data.db.EntryType
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.DateTimePickerDialog
import com.personal.journal.ui.components.DestructiveIconButton
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.components.SoftButton
import com.personal.journal.ui.components.SoftTextField
import com.personal.journal.ui.components.TagLabel
import com.personal.journal.ui.components.TagRow
import com.personal.journal.ui.longWhenLabel
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun EntryScreen(vm: EntryViewModel, photoFile: (String) -> File, onBack: () -> Unit) {
    val e by vm.entry.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    val current = e ?: return Box(Modifier.fillMaxSize().background(JournalColors.Background))
    val entry = current.entry
    val scope = rememberCoroutineScope()
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var draft by remember(entry.id, vm.editTags) { mutableStateOf(entry.text.orEmpty()) }
    val editing = vm.editTags

    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (entry.type == EntryType.PHOTO && entry.photoFile != null) {
                Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 14.dp).clip(JournalShapes.photo)) {
                    AsyncImage(
                        photoFile(entry.photoFile), contentDescription = entry.text ?: "Photo",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).background(JournalColors.Surface),
                    )
                    CircleIconButton(JournalIcons.Back, "Back", onBack, Modifier.padding(4.dp), background = JournalColors.Scrim)
                }
            } else {
                CircleIconButton(JournalIcons.Back, "Back", onBack, Modifier.padding(start = 6.dp, top = 12.dp))
            }
            Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(longWhenLabel(entry.takenAt), style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
                if (editing == null) {
                    entry.text?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = JournalColors.TextPrimary,
                        )
                    }
                    if (current.tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { current.tags.forEach { TagLabel(it.name) } }
                    }
                } else {
                    val tagState by editing.state.collectAsStateWithLifecycle()
                    SoftTextField(draft, { draft = it }, if (entry.type == EntryType.TEXT) "Write something" else "Add a caption", Modifier.fillMaxWidth())
                    TagRow(
                        tagState, onToggle = editing::toggle, onQuery = editing::query, onCreate = editing::create,
                        onOpenSearch = editing::openSearch, onCloseSearch = editing::closeSearch,
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editing == null) {
                SoftButton("Edit", onClick = vm::startEdit, icon = JournalIcons.Pen)
                SoftButton("Date", onClick = { pickDate = true }, icon = JournalIcons.Calendar)
                Spacer(Modifier.weight(1f))
                DestructiveIconButton("Delete entry", onClick = { confirmDelete = true })
            } else {
                SoftButton("Cancel", onClick = vm::cancelEdit)
                Spacer(Modifier.weight(1f))
                val canSave = entry.type == EntryType.PHOTO || draft.isNotBlank()
                PrimaryButton("Save", enabled = canSave, onClick = { scope.launch { vm.saveEdit(draft) } })
            }
        }
    }

    if (pickDate) {
        DateTimePickerDialog(entry.takenAt, onPicked = { pickDate = false; scope.launch { vm.updateDate(it) } }, onDismiss = { pickDate = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = JournalColors.Surface,
            title = { Text("Delete this entry?", color = JournalColors.TextPrimary) },
            text = {
                Text(
                    "The photo and caption will be removed from this phone and from your backup.",
                    color = JournalColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; scope.launch { vm.delete(); onBack() } }) {
                    Text("Delete", color = JournalColors.DestructiveIcon)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = JournalColors.TextPrimary) } },
        )
    }
}
