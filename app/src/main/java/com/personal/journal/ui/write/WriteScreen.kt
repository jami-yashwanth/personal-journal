package com.personal.journal.ui.write

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.DateTimePickerDialog
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.components.TagRow
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import com.personal.journal.ui.whenLabel
import kotlinx.coroutines.launch

@Composable
fun WriteScreen(vm: WriteViewModel, onDone: () -> Unit, onClose: () -> Unit) {
    val tagState by vm.tags.state.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun close() = if (vm.hasUnsavedText) confirmDiscard = true else onClose()
    BackHandler(onBack = ::close)

    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(JournalIcons.Close, "Discard and go back", ::close)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.height(34.dp).clickable(role = Role.Button, onClickLabel = "Change time") { pickTime = true }
                        .clip(JournalShapes.pill).background(JournalColors.Card)
                        .border(1.5.dp, JournalColors.Line, JournalShapes.pill)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(whenLabel(vm.takenAt), style = MaterialTheme.typography.bodySmall, color = JournalColors.TextSecondary) }
            }
            // Balances the close button so the date chip sits centred.
            Spacer(Modifier.size(48.dp))
        }
        BasicTextField(
            value = vm.text,
            onValueChange = vm::updateText,
            textStyle = MaterialTheme.typography.headlineLarge.copy(color = JournalColors.TextPrimary),
            cursorBrush = SolidColor(JournalColors.Action),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 22.dp, vertical = 18.dp).focusRequester(focus),
            decorationBox = { inner ->
                if (vm.text.isEmpty()) {
                    Text("What happened today?", style = MaterialTheme.typography.headlineLarge, color = JournalColors.TextMuted)
                }
                inner()
            },
        )
        TagRow(
            tagState,
            onToggle = vm.tags::toggle, onQuery = vm.tags::query, onCreate = vm.tags::create,
            onOpenSearch = vm.tags::openSearch, onCloseSearch = vm.tags::closeSearch,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        PrimaryButton(
            if (vm.savesToToday) "Save to today" else "Save", enabled = vm.canSave, arrow = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp, bottom = 16.dp),
            onClick = { scope.launch { if (vm.save() != null) onDone() } },
        )
    }

    if (pickTime) {
        DateTimePickerDialog(vm.takenAt, onPicked = { vm.updateTakenAt(it); pickTime = false }, onDismiss = { pickTime = false })
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = JournalColors.Card,
            title = { Text("Discard this entry?", color = JournalColors.TextPrimary) },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onClose() }) { Text("Discard", color = JournalColors.TextPrimary) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep writing", color = JournalColors.TextPrimary) } },
        )
    }
}
