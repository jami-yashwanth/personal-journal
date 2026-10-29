package com.personal.journal.ui.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.DateTimePickerDialog
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.components.SoftTextField
import com.personal.journal.ui.components.TagRow
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import com.personal.journal.ui.whenLabel
import kotlinx.coroutines.launch
import android.net.Uri

@Composable
fun ImportScreen(vm: ImportViewModel, uris: List<Uri>, onDone: () -> Unit, onCancel: () -> Unit) {
    LaunchedEffect(uris) { vm.load(uris) }
    val items by vm.items.collectAsStateWithLifecycle()
    val tagState by vm.tags.state.collectAsStateWithLifecycle()
    var pickFor by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val n = uris.size
    val readable = items.count { it.error == null }

    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Add $n ${if (n == 1) "photo" else "photos"}",
                style = MaterialTheme.typography.headlineSmall, color = JournalColors.TextPrimary, modifier = Modifier.weight(1f),
            )
            CircleIconButton(JournalIcons.Close, "Cancel import", onCancel)
        }
        Text(
            "Each photo becomes its own entry, on the day it was taken.",
            style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyColumn(
            Modifier.weight(1f).padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(items, key = { i, item -> "$i-${item.uri}" }) { i, item ->
                Row(
                    Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(JournalColors.Card).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AsyncImage(
                        item.uri, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)).background(JournalColors.Sunk),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.error != null) {
                            Text(item.error, style = MaterialTheme.typography.bodySmall, color = JournalColors.Danger)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(whenLabel(item.takenAt), style = MaterialTheme.typography.labelMedium, color = JournalColors.TextPrimary, modifier = Modifier.weight(1f))
                                Text(
                                    "Change",
                                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                                    color = JournalColors.TextSecondary,
                                    modifier = Modifier.clickable(role = Role.Button) { pickFor = i }.padding(6.dp),
                                )
                            }
                            if (!item.hasExifDate && !item.dateEdited) {
                                Text("No date in this photo, so it's set to today.", style = MaterialTheme.typography.labelSmall.copy(fontWeight = null), color = JournalColors.TextMuted)
                            }
                            SoftTextField(
                                item.caption, { vm.setCaption(i, it) }, "Add a caption",
                                minHeight = 38.dp, shape = RoundedCornerShape(12.dp),
                                style = MaterialTheme.typography.bodyMedium, background = JournalColors.Sunk,
                            )
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp).padding(top = 8.dp)) {
            Text("Tags for all $n", style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted, modifier = Modifier.padding(start = 4.dp))
            TagRow(
                tagState, onToggle = vm.tags::toggle, onQuery = vm.tags::query, onCreate = vm.tags::create,
                onOpenSearch = vm.tags::openSearch, onCloseSearch = vm.tags::closeSearch,
            )
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JournalColors.Danger, modifier = Modifier.padding(4.dp)) }
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
                PrimaryButton(
                    "Add $readable ${if (readable == 1) "entry" else "entries"}",
                    enabled = readable > 0 && !saving,
                    modifier = Modifier.fillMaxWidth(),
                    height = 54.dp,
                    onClick = {
                        saving = true
                        scope.launch {
                            val r = vm.saveAll()
                            if (r.failed.isEmpty() || r.saved > 0 && r.failed.all { items[it].error != null }) onDone()
                            else {
                                message = "${r.failed.size} couldn't be saved. The others were added."
                                saving = false
                            }
                        }
                    },
                )
            }
        }
    }
    pickFor?.let { i ->
        DateTimePickerDialog(items[i].takenAt, onPicked = { vm.setDate(i, it); pickFor = null }, onDismiss = { pickFor = null })
    }
}
