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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.journal.ui.components.CircleIconButton
import com.personal.journal.ui.components.JournalIcons
import com.personal.journal.ui.components.PrimaryButton
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalShapes
import java.io.File
import java.util.UUID

@Composable
fun CaptureScreen(
    vm: CaptureViewModel,
    onOpenMonth: () -> Unit,
    onCaptured: (File) -> Unit,
    onImport: (List<Uri>) -> Unit,
    onWrite: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val count by vm.todayCount.collectAsStateWithLifecycle()

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) {
        if (!granted && !asked) {
            asked = true
            permission.launch(Manifest.permission.CAMERA)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshDay()
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
    LaunchedEffect(granted) { if (granted) controller.bindToLifecycle(lifecycleOwner) }
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

    Column(
        Modifier.fillMaxSize().background(JournalColors.Background).statusBarsPadding().navigationBarsPadding(),
    ) {
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(start = 12.dp, end = 12.dp, top = 14.dp)
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

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 16.dp).height(60.dp)
                .clip(RoundedTodayShape).background(JournalColors.Surface)
                .clickable(role = Role.Button, onClick = onOpenMonth).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                buildAnnotatedString {
                    append("Today")
                    withStyle(SpanStyle(color = JournalColors.TextMuted)) { append(", $count ${if (count == 1) "entry" else "entries"}") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = JournalColors.TextPrimary,
            )
            Icon(JournalIcons.ChevronRight, contentDescription = null, tint = JournalColors.TextMuted, modifier = Modifier.size(18.dp))
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 28.dp),
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

private val RoundedTodayShape = JournalShapes.tile

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
