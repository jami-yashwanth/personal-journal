package com.personal.journal.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle

/** The path of a photo Review just saved, left on the camera's back-stack entry for its shrink-into-the-handle. */
const val JustSaved = "justSaved"

/** Reads [key] and removes it, so whatever it triggers plays once. */
fun SavedStateHandle.consumeOnce(key: String): String? = remove<String>(key)

/**
 * The photo to shrink into the camera's handle, resolved from the id Review left on [handle]. Held only while the camera
 * stays on screen: coming back to it later shows nothing again.
 */
@Composable
fun rememberJustSaved(handle: SavedStateHandle, resolve: suspend (entryId: String) -> String?): MutableState<String?> {
    // Not saveable on purpose: a restored camera has nothing to replay, and the handle has already let go of the id.
    val saved = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { handle.consumeOnce(JustSaved)?.let { saved.value = resolve(it) } }
    return saved
}
