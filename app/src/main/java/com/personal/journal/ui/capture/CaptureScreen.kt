package com.personal.journal.ui.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.abs
import java.io.File
import java.util.UUID

@Composable
fun CaptureScreen(
    cameraEnabled: Boolean,
    onOpenMonth: () -> Unit,
    onCaptured: (File) -> Unit,
    onImport: (List<Uri>) -> Unit,
    onWrite: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    // Never ask while the lock screen is up: the permission dialog would fight the unlock prompt.
    LaunchedEffect(cameraEnabled) {
        if (cameraEnabled && !granted && !asked) {
            asked = true
            permission.launch(Manifest.permission.CAMERA)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val controller = remember { LifecycleCameraController(context) }
    var flashOn by rememberSaveable { mutableStateOf(false) }
    var front by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(granted, cameraEnabled) {
        if (granted && cameraEnabled) controller.bindToLifecycle(lifecycleOwner) else controller.unbind()
    }
    LaunchedEffect(flashOn) { controller.imageCaptureFlashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF }
    LaunchedEffect(front) {
        controller.cameraSelector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) onImport(uris)
    }

    fun shoot() {
        if (busy || !granted) return
        busy = true
        val file = File(context.cacheDir, "capture-${UUID.randomUUID()}.jpg")
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    busy = false
                    onCaptured(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    busy = false
                    file.delete()
                }
            },
        )
    }

    val openMonth by rememberUpdatedState(onOpenMonth)

    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding(),
    ) {
        Box(
            Modifier.fillMaxWidth().weight(1f).swipeUpToOpen { openMonth() }.padding(start = 12.dp, end = 12.dp, top = 14.dp)
                .clip(JournalShapes.photo).background(JournalColors.Surface),
        ) {
            if (granted) {
                AndroidView(
                    factory = { PreviewView(it).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } },
                    modifier = Modifier.fillMaxSize(),
                )
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    CircleIconButton(
                        if (flashOn) JournalIcons.FlashOn else JournalIcons.FlashOff,
                        if (flashOn) "Flash on" else "Flash off",
                        { flashOn = !flashOn },
                        background = JournalColors.Scrim,
                    )
                    CircleIconButton(
                        JournalIcons.Flip,
                        if (front) "Switch to back camera" else "Switch to front camera",
                        { front = !front },
                        background = JournalColors.Scrim,
                    )
                }
            } else {
                CameraOff(onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                })
            }
        }

        // The only hint that the journal sits below the camera: swipe up anywhere above it, or tap it.
        Box(
            Modifier.align(Alignment.CenterHorizontally).size(width = 72.dp, height = 40.dp).swipeUpToOpen { openMonth() }
                .clip(JournalShapes.pill)
                .clickable(role = Role.Button, onClick = onOpenMonth)
                .semantics { contentDescription = "Open journal" },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = 36.dp, height = 4.dp).clip(JournalShapes.pill).background(JournalColors.TextMuted))
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 4.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                JournalIcons.Gallery, "Add photos from gallery",
                { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                size = 52.dp,
            )
            Box(
                Modifier.size(76.dp).clip(CircleShape).border(3.dp, JournalColors.PrimaryFill, CircleShape)
                    .clickable(enabled = granted && !busy, role = Role.Button, onClickLabel = "Take photo", onClick = ::shoot),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(62.dp).clip(CircleShape)
                        .background(if (granted) JournalColors.PrimaryFill else JournalColors.SurfaceRaised),
                )
            }
            CircleIconButton(JournalIcons.Pen, "Write an entry", onWrite, size = 52.dp)
        }
    }
}

/**
 * Calls [onOpen] on a deliberate upward drag. Listens on the Initial pass because the camera preview (a PreviewView)
 * consumes touches for tap-to-focus and pinch-to-zoom; a second finger means a pinch, so the gesture is dropped.
 * Only applied to the area above the shutter row, which never reaches the system's bottom home-gesture zone.
 */
private fun Modifier.swipeUpToOpen(onOpen: () -> Unit): Modifier = pointerInput(Unit) {
    val threshold = SwipeUpThreshold.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var drag = Offset.Zero
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.size > 1) return@awaitEachGesture
            val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
            if (!change.pressed) return@awaitEachGesture
            drag += change.positionChange()
            if (-drag.y > threshold && -drag.y > 2 * abs(drag.x)) {
                change.consume()
                onOpen()
                return@awaitEachGesture
            }
        }
    }
}

private val SwipeUpThreshold = 80.dp

@Composable
private fun CameraOff(onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(JournalIcons.CameraOff, contentDescription = null, tint = JournalColors.TextMuted, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text("Camera access is off", style = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = JournalColors.TextPrimary)
        Spacer(Modifier.height(10.dp))
        Text(
            "Allow camera access in Android settings to take photos here. You can still write, or add photos from your gallery.",
            style = MaterialTheme.typography.bodyMedium,
            color = JournalColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Open settings", onOpenSettings, height = 48.dp)
    }
}
