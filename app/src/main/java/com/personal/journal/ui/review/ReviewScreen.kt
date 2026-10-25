package com.personal.journal.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.components.SoftTextField
import com.personal.journal.ui.components.TagRow
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import com.personal.journal.ui.whenLabel
import kotlinx.coroutines.launch

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
        Box(Modifier.fillMaxWidth().weight(1f).padding(start = 12.dp, end = 12.dp, top = 14.dp).clip(JournalShapes.photo)) {
            AsyncImage(
                model = vm.photo,
                contentDescription = "Photo just taken",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(JournalColors.Surface),
            )
            Box(
                Modifier.padding(12.dp).height(40.dp).clip(JournalShapes.pill).background(JournalColors.Scrim)
                    .clickable(role = Role.Button, onClick = ::retake).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Retake", style = MaterialTheme.typography.bodyMedium, color = JournalColors.TextPrimary) }
        }
        Column(Modifier.padding(horizontal = 12.dp).padding(top = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftTextField(caption, { caption = it }, "Add a caption", modifier = Modifier.fillMaxWidth())
            TagRow(
                tagState,
                onToggle = vm.tags::toggle, onQuery = vm.tags::query, onCreate = vm.tags::create,
                onOpenSearch = vm.tags::openSearch, onCloseSearch = vm.tags::closeSearch,
            )
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JournalColors.DestructiveIcon) }
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(whenLabel(vm.takenAt), style = MaterialTheme.typography.bodySmall, color = JournalColors.TextMuted)
                PrimaryButton("Save", enabled = !saving, onClick = {
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
