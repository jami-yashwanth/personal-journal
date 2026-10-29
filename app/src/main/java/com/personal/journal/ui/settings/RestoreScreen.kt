package com.personal.journal.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.personal.journal.backup.BackupException
import com.personal.journal.backup.BackupManager
import com.personal.journal.backup.InvalidPhraseException
import com.personal.journal.backup.NoBackupException
import com.personal.journal.backup.R2Credentials
import com.personal.journal.backup.RestoreRunner
import com.personal.journal.backup.WrongPhraseException
import com.personal.journal.data.PhotoStore
import com.personal.journal.data.db.JournalDatabase
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import kotlinx.coroutines.launch

class RestoreViewModel(
    private val manager: BackupManager,
    private val db: JournalDatabase,
    private val photos: PhotoStore,
) : ViewModel() {
    private val saved = manager.credentials.loadR2()
    var accountId by mutableStateOf(saved?.accountId.orEmpty())
    var accessKeyId by mutableStateOf(saved?.accessKeyId.orEmpty())
    var secret by mutableStateOf(saved?.secretAccessKey.orEmpty())
    var bucket by mutableStateOf(saved?.bucket.orEmpty())
    var phraseInput by mutableStateOf(manager.credentials.loadPhrase()?.joinToString(" ").orEmpty())
    var running by mutableStateOf(false)
    var done by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var failed by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val ready get() = listOf(accountId, accessKeyId, secret, bucket, phraseInput).all { it.isNotBlank() }

    suspend fun restore() {
        running = true
        error = null
        try {
            val creds = R2Credentials(accountId.trim(), accessKeyId.trim(), secret.trim(), bucket.trim())
            val words = manager.phrases.parse(phraseInput)
            val verifier = RestoreRunner(db, photos, manager.client(creds), manager.phrases)
            val key = verifier.verify(words)
            // Not connected yet: connect to this bucket first (that marks the whole journal for upload),
            // then restored rows are already in this bucket. Connected elsewhere: restored rows must be uploaded.
            if (!manager.connected.value) manager.connect(creds, words)
            val sameBucket = manager.isCurrentBucket(creds)
            val runner = RestoreRunner(db, photos, manager.client(creds), manager.phrases, markBackedUp = sameBucket)
            runner.restore(key).collect { done = it.done; total = it.total; failed = it.failed }
            if (!sameBucket) manager.backUpNow()
            finished = true
        } catch (e: WrongPhraseException) {
            error = e.message
        } catch (e: NoBackupException) {
            error = e.message
        } catch (e: InvalidPhraseException) {
            error = e.message
        } catch (e: BackupException) {
            error = e.setupMessage()
        } finally {
            running = false
        }
    }
}

@Composable
fun RestoreScreen(vm: RestoreViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.padding(start = 6.dp, top = 12.dp)) { CircleIconButton(JournalIcons.Back, "Back", onBack) }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Restore from backup", style = MaterialTheme.typography.headlineSmall, color = JournalColors.TextPrimary)
            Text(
                "Enter your R2 details and recovery phrase. Entries already on this phone are kept; for the same entry, the newer version wins.",
                style = MaterialTheme.typography.bodyMedium, color = JournalColors.TextSecondary,
            )
            if (vm.finished) {
                Text(
                    "Restored ${vm.total - vm.failed} of ${vm.total} entries." + if (vm.failed > 0) " ${vm.failed} couldn't be read." else "",
                    style = MaterialTheme.typography.bodyLarge, color = JournalColors.TextPrimary,
                )
                PrimaryButton("Done", onDone, modifier = Modifier.fillMaxWidth())
                return@Column
            }
            Input("Account ID", vm.accountId) { vm.accountId = it }
            Input("Access Key ID", vm.accessKeyId) { vm.accessKeyId = it }
            Input("Secret Access Key", vm.secret, secret = true) { vm.secret = it }
            Input("Bucket name", vm.bucket) { vm.bucket = it }
            Input("Recovery phrase (12 words)", vm.phraseInput, multiline = true) { vm.phraseInput = it }
            vm.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JournalColors.Danger) }
            if (vm.running && vm.total > 0) {
                LinearProgressIndicator(
                    progress = { vm.done.toFloat() / vm.total },
                    color = JournalColors.Action, trackColor = JournalColors.Sunk,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Restoring ${vm.done} of ${vm.total}", style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
            }
            PrimaryButton(
                if (vm.running) "Restoring…" else "Restore",
                enabled = vm.ready && !vm.running, modifier = Modifier.fillMaxWidth(),
                onClick = { scope.launch { vm.restore() } },
            )
        }
    }
}

@Composable
private fun Input(label: String, value: String, secret: Boolean = false, multiline: Boolean = false, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = !multiline, minLines = if (multiline) 3 else 1,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = JournalColors.TextPrimary),
            cursorBrush = SolidColor(JournalColors.TextPrimary),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Ascii, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth().clip(JournalShapes.button).background(JournalColors.Card).padding(16.dp),
        )
    }
}
