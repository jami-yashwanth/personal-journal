package com.personal.journal.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ampm = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

/** "Last backup 10 min ago, 142 entries" etc. */
fun backupStatusLine(connected: Boolean, lastSuccess: Long?, count: Int, error: String?, now: Long): String = when {
    !connected -> "Not set up. Your entries are only on this phone."
    error != null -> "Backup failed: $error"
    lastSuccess == null -> "Waiting for the first backup"
    else -> {
        val ago = Duration.ofMillis(now - lastSuccess)
        val rel = when {
            ago.toMinutes() < 1 -> "just now"
            ago.toHours() < 1 -> "${ago.toMinutes()} min ago"
            ago.toDays() < 1 -> "${ago.toHours()} h ago"
            else -> "${ago.toDays()} days ago"
        }
        "Last backup $rel, $count ${if (count == 1) "entry" else "entries"}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    lockAvailable: Boolean,
    onBack: () -> Unit,
    onSetUpBackup: () -> Unit,
    onRestore: () -> Unit,
    onShowPhrase: (onVerified: () -> Unit) -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val state = s ?: return Box(Modifier.fillMaxSize().background(JournalColors.Background))
    var pickTime by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var phrase by remember { mutableStateOf<List<String>?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.padding(start = 6.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(JournalIcons.Back, "Back", onBack)
            Text("Settings", style = MaterialTheme.typography.headlineSmall, color = JournalColors.TextPrimary, modifier = Modifier.padding(start = 6.dp))
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card {
                ToggleRow(
                    "Lock the journal",
                    if (lockAvailable) "Fingerprint or PIN each time you open it" else "Set a screen lock in Android settings to use this",
                    checked = state.lock && lockAvailable, enabled = lockAvailable, onChange = vm::setLock,
                )
            }
            Card {
                ToggleRow("Daily reminder", "\"Anything from today?\"", state.reminder, onChange = vm::setReminder)
                if (state.reminder) {
                    Divider()
                    ValueRow("Time", ampm.format(state.reminderTime).lowercase()) { pickTime = true }
                }
            }
            Card {
                TextRow(
                    "Backup",
                    backupStatusLine(state.backupConnected, state.backup.lastSuccessAt, state.backup.entryCount, state.backup.error, System.currentTimeMillis()),
                    error = state.backupConnected && state.backup.error != null,
                )
                Divider()
                if (!state.backupConnected) {
                    ActionRow("Set up backup", onSetUpBackup)
                    Divider()
                    ActionRow("Restore from backup", onRestore)
                } else {
                    ActionRow("Back up now", vm.backup::backUpNow)
                    Divider()
                    ToggleRow("Use mobile data", "Off: backs up only on Wi-Fi", state.useMobileData, onChange = vm::setUseMobileData)
                    Divider()
                    ActionRow("Show recovery phrase", onClick = { onShowPhrase { phrase = vm.backup.recoveryPhrase() } })
                    Divider()
                    ActionRow("Restore from backup", onRestore)
                    Divider()
                    ActionRow("Disconnect backup", onClick = { confirmDisconnect = true }, destructive = true)
                }
            }
            Text(state.storageLine, style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted, modifier = Modifier.padding(10.dp))
        }
    }

    if (pickTime) {
        val t = rememberTimePickerState(state.reminderTime.hour, state.reminderTime.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            containerColor = JournalColors.Surface,
            confirmButton = { TextButton(onClick = { vm.setReminderTime(LocalTime.of(t.hour, t.minute)); pickTime = false }) { Text("Done", color = JournalColors.TextPrimary) } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel", color = JournalColors.TextSecondary) } },
            text = { TimePicker(t) },
        )
    }
    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            containerColor = JournalColors.Surface,
            title = { Text("Disconnect backup?", color = JournalColors.TextPrimary) },
            text = { Text("New entries won't be backed up. Your existing backup in R2 is not deleted.", color = JournalColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { confirmDisconnect = false; scope.launch { vm.backup.disconnect() } }) {
                    Text("Disconnect", color = JournalColors.DestructiveIcon)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel", color = JournalColors.TextPrimary) } },
        )
    }
    phrase?.let { words ->
        AlertDialog(
            onDismissRequest = { phrase = null },
            containerColor = JournalColors.Surface,
            title = { Text("Recovery phrase", color = JournalColors.TextPrimary) },
            text = { PhraseGrid(words) },
            confirmButton = { TextButton(onClick = { phrase = null }) { Text("Done", color = JournalColors.TextPrimary) } },
        )
    }
}

@Composable
fun PhraseGrid(words: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        words.chunked(3).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEachIndexed { i, w ->
                    Text(
                        "${row * 3 + i + 1}. $w",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JournalColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().clip(JournalShapes.card).background(JournalColors.Surface), content = content)

@Composable
private fun Divider() = HorizontalDivider(color = JournalColors.Hairline, modifier = Modifier.padding(horizontal = 16.dp))

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = JournalColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
        }
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = JournalColors.OnPrimary, checkedTrackColor = JournalColors.PrimaryFill,
                uncheckedThumbColor = JournalColors.TextMuted, uncheckedTrackColor = JournalColors.SurfaceRaised,
                uncheckedBorderColor = JournalColors.Hairline,
            ),
        )
    }
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = JournalColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = JournalColors.TextPrimary)
    }
}

@Composable
private fun TextRow(title: String, subtitle: String, error: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = JournalColors.TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (error) JournalColors.DestructiveIcon else JournalColors.TextMuted)
    }
}

@Composable
private fun ActionRow(title: String, onClick: () -> Unit, destructive: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title, style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) JournalColors.DestructiveIcon else JournalColors.TextPrimary, modifier = Modifier.weight(1f),
        )
        if (!destructive) androidx.compose.material3.Icon(JournalIcons.ChevronRight, null, tint = JournalColors.TextMuted)
    }
}
