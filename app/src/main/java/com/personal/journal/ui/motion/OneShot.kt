package com.personal.journal.ui.motion

import androidx.lifecycle.SavedStateHandle

/** The path of a photo Review just saved, left on the camera's back-stack entry for its shrink-into-the-handle. */
const val JustSaved = "justSaved"

/** Reads [key] and removes it, so whatever it triggers plays once. */
fun SavedStateHandle.consumeOnce(key: String): String? = remove<String>(key)
