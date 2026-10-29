package com.personal.journal.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/** Returns a function that opens the gallery picker for up to 20 photos; [onPicked] is not called when none are chosen. */
@Composable
fun rememberImportPicker(onPicked: (List<Uri>) -> Unit): () -> Unit {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) onPicked(uris)
    }
    return { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
