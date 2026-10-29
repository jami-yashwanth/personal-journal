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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.personal.journal.backup.R2Credentials
import com.personal.journal.backup.WrongPhraseException
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import kotlinx.coroutines.launch

enum class SetupStep { FORM, SHOW_PHRASE, CONFIRM_PHRASE, EXISTING_PHRASE }

class BackupSetupViewModel(val manager: BackupManager) : ViewModel() {
    var step by mutableStateOf(SetupStep.FORM)
    var accountId by mutableStateOf("")
    var accessKeyId by mutableStateOf("")
    var secret by mutableStateOf("")
    var bucket by mutableStateOf("")
    var phraseInput by mutableStateOf("")
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var phrase: List<String> = emptyList()
        private set

    private fun creds() = R2Credentials(accountId.trim(), accessKeyId.trim(), secret.trim(), bucket.trim())

    val formComplete get() = listOf(accountId, accessKeyId, secret, bucket).all { it.isNotBlank() }

    suspend fun testConnection() = guarded {
        val existing = manager.testConnection(creds())
        if (existing) {
            step = SetupStep.EXISTING_PHRASE
        } else {
            phrase = manager.newPhrase()
            step = SetupStep.SHOW_PHRASE
        }
    }

    /** Returns true when backup is now on. */
    suspend fun finish(): Boolean {
        var ok = false
        guarded {
            val typed = manager.phrases.parse(phraseInput)
            if (step == SetupStep.CONFIRM_PHRASE && typed != phrase) {
                error = "That doesn't match the phrase shown. Check the words and their order."
                return@guarded
            }
            manager.connect(creds(), typed)
            ok = true
        }
        return ok
    }

    private suspend fun guarded(block: suspend () -> Unit) {
        busy = true
        error = null
        try {
            block()
        } catch (e: BackupException) {
            error = e.setupMessage()
        } catch (e: WrongPhraseException) {
            error = e.message
        } catch (e: InvalidPhraseException) {
            error = e.message
        } finally {
            busy = false
        }
    }
}

@Composable
fun BackupSetupScreen(vm: BackupSetupViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.padding(start = 6.dp, top = 12.dp)) {
            CircleIconButton(JournalIcons.Back, "Back", onBack)
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (vm.step) {
                SetupStep.FORM -> {
                    Title("Set up backup", "Your journal is encrypted on this phone before it goes to your Cloudflare R2 bucket. Create an R2 API token limited to one bucket with read and write access, then paste its details here.")
                    Field("Account ID", vm.accountId) { vm.accountId = it }
                    Field("Access Key ID", vm.accessKeyId) { vm.accessKeyId = it }
                    Field("Secret Access Key", vm.secret, secret = true) { vm.secret = it }
                    Field("Bucket name", vm.bucket) { vm.bucket = it }
                    Error(vm.error)
                    PrimaryButton(if (vm.busy) "Testing…" else "Test connection", enabled = vm.formComplete && !vm.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                        scope.launch { vm.testConnection() }
                    })
                }
                SetupStep.SHOW_PHRASE -> {
                    Title("Your recovery phrase", "Write these 12 words down and keep them somewhere safe, like your password manager. You need them to restore your journal on a new phone. Without them the backup can't be opened.")
                    Column(Modifier.fillMaxWidth().clip(JournalShapes.card).background(JournalColors.Card).padding(18.dp)) { PhraseGrid(vm.phrase) }
                    PrimaryButton("I've written it down", modifier = Modifier.fillMaxWidth(), onClick = { vm.step = SetupStep.CONFIRM_PHRASE })
                }
                SetupStep.CONFIRM_PHRASE, SetupStep.EXISTING_PHRASE -> {
                    if (vm.step == SetupStep.CONFIRM_PHRASE) {
                        Title("Type the phrase", "Type the 12 words to confirm you saved them.")
                    } else {
                        Title("This bucket already has a backup", "Enter its recovery phrase to keep backing up to it.")
                    }
                    Field("12 words, separated by spaces", vm.phraseInput, multiline = true) { vm.phraseInput = it }
                    Error(vm.error)
                    PrimaryButton(if (vm.busy) "Checking…" else "Turn on backup", enabled = vm.phraseInput.isNotBlank() && !vm.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                        scope.launch { if (vm.finish()) onDone() }
                    })
                }
            }
        }
    }
}

@Composable
private fun Title(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = JournalColors.TextPrimary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = JournalColors.TextSecondary)
    }
}

@Composable
private fun Error(message: String?) {
    if (message != null) Text(message, style = MaterialTheme.typography.bodySmall, color = JournalColors.Danger)
}

@Composable
private fun Field(label: String, value: String, secret: Boolean = false, multiline: Boolean = false, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = JournalColors.TextPrimary),
            cursorBrush = SolidColor(JournalColors.TextPrimary),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Ascii, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth().clip(JournalShapes.button).background(JournalColors.Card).padding(16.dp),
        )
    }
}
