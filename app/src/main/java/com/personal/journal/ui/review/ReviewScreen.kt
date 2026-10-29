package com.personal.journal.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.components.SoftButton
import com.personal.journal.ui.components.TagRow
import com.personal.journal.ui.theme.JournalColors
import kotlinx.coroutines.launch

private val PhotoShape = RoundedCornerShape(36.dp)

@Composable
fun ReviewScreen(vm: ReviewViewModel, onDone: () -> Unit, onRetake: () -> Unit) {
    var caption by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val tagState by vm.tags.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    fun retake() {
        vm.discard()
        onRetake()
    }
    BackHandler(onBack = ::retake)

    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        Box(Modifier.fillMaxWidth().weight(1f).padding(start = 12.dp, end = 12.dp, top = 14.dp).clip(PhotoShape)) {
            AsyncImage(
                model = vm.photo,
                contentDescription = "Photo just taken",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(JournalColors.Sunk),
            )
        }
        Column(Modifier.padding(horizontal = 12.dp).padding(top = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicTextField(
                value = caption,
                onValueChange = { caption = it },
                textStyle = MaterialTheme.typography.titleLarge.copy(color = JournalColors.TextPrimary),
                cursorBrush = SolidColor(JournalColors.Action),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                decorationBox = { inner ->
                    if (caption.isEmpty()) {
                        Text("Add a line about this…", style = MaterialTheme.typography.titleLarge, color = JournalColors.TextMuted)
                    }
                    inner()
                },
            )
            TagRow(
                tagState,
                onToggle = vm.tags::toggle, onQuery = vm.tags::query, onCreate = vm.tags::create,
                onOpenSearch = vm.tags::openSearch, onCloseSearch = vm.tags::closeSearch,
            )
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JournalColors.Danger) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SoftButton("Retake", ::retake, Modifier.weight(1f), height = 56.dp)
                PrimaryButton("Save to today", enabled = !saving, arrow = true, modifier = Modifier.weight(1.7f), onClick = {
                    saving = true
                    scope.launch {
                        runCatching { vm.save(caption) }
                            .onSuccess { onDone() }
                            .onFailure { error = it.message ?: "Couldn't save this photo"; saving = false }
                    }
                })
            }
        }
    }
}
