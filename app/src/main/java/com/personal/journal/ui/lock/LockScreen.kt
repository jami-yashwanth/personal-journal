package com.personal.journal.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding()
            .padding(16.dp)
            // Swallow every touch so nothing reaches the journal drawn underneath.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
        verticalArrangement = Arrangement.Center,
    ) {
        // 470dp is the target; on short or landscape screens it shrinks so Unlock stays visible.
        Box(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 470.dp).fillMaxHeight().clip(JournalShapes.hero).background(JournalColors.Danger).padding(28.dp)) {
            Box(
                Modifier.align(Alignment.TopEnd).size(84.dp).clip(CircleShape).background(LockInk),
                contentAlignment = Alignment.Center,
            ) {
                Icon(JournalIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Column(Modifier.align(Alignment.BottomStart)) {
                Text("Your journal", style = MaterialTheme.typography.displayLarge, color = JournalColors.OnColour)
                Spacer(Modifier.height(8.dp))
                Text("Touch the sensor to open it.", style = MaterialTheme.typography.bodyLarge, color = JournalColors.OnColour)
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Unlock", onUnlock, Modifier.fillMaxWidth(), arrow = true)
    }
}

/** The ink circle on the coral block; fixed in both themes like the other on-colour ink. */
private val LockInk = Color(0xFF1E1B19)
