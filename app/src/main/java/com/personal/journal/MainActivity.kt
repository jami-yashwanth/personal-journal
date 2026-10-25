package com.personal.journal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.personal.journal.ui.appContainer
import com.personal.journal.ui.capture.CaptureScreen
import com.personal.journal.ui.day.DayScreen
import com.personal.journal.ui.day.DayViewModel
import com.personal.journal.ui.entry.EntryScreen
import com.personal.journal.ui.entry.EntryViewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.personal.journal.ui.importer.ImportScreen
import com.personal.journal.ui.importer.ImportViewModel
import com.personal.journal.ui.month.MonthScreen
import com.personal.journal.ui.month.MonthViewModel
import com.personal.journal.ui.review.ReviewScreen
import com.personal.journal.ui.review.ReviewViewModel
import com.personal.journal.ui.theme.JournalColors
import com.personal.journal.ui.theme.JournalTheme
import com.personal.journal.ui.write.WriteScreen
import com.personal.journal.ui.write.WriteViewModel
import android.Manifest
import android.os.Build
import android.view.WindowManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import com.personal.journal.ui.lock.Biometrics
import com.personal.journal.ui.lock.LockScreen
import com.personal.journal.data.BackupStatus
import com.personal.journal.ui.settings.BackupSetupScreen
import com.personal.journal.ui.settings.BackupSetupViewModel
import com.personal.journal.ui.settings.RestoreScreen
import com.personal.journal.ui.settings.RestoreViewModel
import com.personal.journal.ui.settings.SettingsScreen
import com.personal.journal.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

object Routes {
    const val CAPTURE = "capture"
    const val REVIEW = "review?path={path}"
    const val WRITE = "write"
    const val MONTH = "month"
    const val IMPORT = "import"
    const val SETTINGS = "settings"
    const val DAY = "day/{date}"
    const val ENTRY = "entry/{id}"
    const val BACKUP_SETUP = "backup-setup"
    const val RESTORE = "restore"
    val WITH_UNSAVED_WORK = setOf(REVIEW, WRITE, IMPORT, ENTRY, BACKUP_SETUP, RESTORE)
    fun review(file: File) = "review?path=${Uri.encode(file.path)}"
    fun day(date: String) = "day/$date"
    fun entry(id: String) = "entry/$id"
}

class MainActivity : FragmentActivity() {
    private var shortcut by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shortcut = intent?.action
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("seed", false) == true) {
            val container = (application as JournalApp).container
            container.appScope.launch { DebugSeeder.seed(container.repository) }
        }
        val container = (application as JournalApp).container
        setContent {
            JournalTheme {
                val nav = rememberNavController()
                LaunchedEffect(shortcut) {
                    when (shortcut) {
                        ACTION_WRITE -> nav.navigate(Routes.WRITE) { launchSingleTop = true }
                        // Don't throw away a draft, an unsaved photo or a setup in progress.
                        ACTION_CAPTURE -> if (nav.currentDestination?.route !in Routes.WITH_UNSAVED_WORK) {
                            nav.popBackStack(Routes.CAPTURE, inclusive = false)
                        }
                    }
                    shortcut = null
                }
                val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                val lockOn by container.settings.lockEnabled.collectAsState(initial = null)
                Box(Modifier.fillMaxSize().background(JournalColors.Background)) {
                    val needsLock = lockOn != false && Biometrics.available(this@MainActivity) && container.lockGate.locked
                    // Ask for notifications only once the journal is open, never on top of the lock prompt.
                    LaunchedEffect(needsLock, lockOn) {
                        if (lockOn != null && !needsLock && Build.VERSION.SDK_INT >= 33 &&
                            container.settings.reminderEnabled.first() && container.settings.firstNotificationAsk()
                        ) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    val hidden = needsLock || lockOn == null
                    // While locked the journal stays composed (keeps navigation) but is invisible to screen readers.
                    Box(if (hidden) Modifier.fillMaxSize().clearAndSetSemantics { } else Modifier.fillMaxSize()) {
                        JournalNavHost(nav, this@MainActivity, cameraEnabled = !hidden)
                    }
                    LaunchedEffect(lockOn) { hideFromRecents(lockOn != false) }
                    if (lockOn == null || needsLock) {
                        LockScreen(onUnlock = {
                            if (lockOn == true) Biometrics.prompt(this@MainActivity, "Unlock journal") { container.lockGate.unlocked() }
                        })
                    }
                }
            }
        }
    }

    /** Keeps journal content out of the app-switcher thumbnail while the lock is on. */
    private fun hideFromRecents(hide: Boolean) {
        if (Build.VERSION.SDK_INT >= 33) {
            setRecentsScreenshotEnabled(!hide)
        } else if (hide) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        shortcut = intent.action
    }

    companion object {
        const val ACTION_CAPTURE = "com.personal.journal.CAPTURE"
        const val ACTION_WRITE = "com.personal.journal.WRITE"
    }
}

/** Timing shared by the camera-to-journal slide, so both screens move as one. */
private fun <T> sheet() = tween<T>(durationMillis = 320, easing = FastOutSlowInEasing)

@Composable
private fun JournalNavHost(nav: NavHostController, activity: FragmentActivity, cameraEnabled: Boolean) {
    val container = appContainer()
    // Lives as long as the activity so Month is already loaded when it slides up over the camera.
    val monthVm = viewModel(viewModelStoreOwner = activity) { MonthViewModel(container.repository) }
    NavHost(
        nav,
        startDestination = Routes.CAPTURE,
        modifier = Modifier.fillMaxSize().background(JournalColors.Background),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(
            Routes.CAPTURE,
            // The camera lifts away as the journal rises over it, and settles back when the journal drops.
            exitTransition = { if (targetState.destination.route == Routes.MONTH) slideOutVertically(sheet()) { -it / 4 } + fadeOut(sheet()) else fadeOut() },
            popEnterTransition = { if (initialState.destination.route == Routes.MONTH) slideInVertically(sheet()) { -it / 4 } + fadeIn(sheet()) else fadeIn() },
        ) {
            CaptureScreen(
                cameraEnabled = cameraEnabled,
                onOpenMonth = { monthVm.showCurrentMonth(); nav.navigate(Routes.MONTH) },
                onCaptured = { nav.navigate(Routes.review(it)) },
                onImport = { container.pendingImport = it; nav.navigate(Routes.IMPORT) },
                onWrite = { nav.navigate(Routes.WRITE) },
            )
        }
        composable(Routes.REVIEW, arguments = listOf(navArgument("path") { type = NavType.StringType })) { entry ->
            val path = entry.arguments?.getString("path").orEmpty()
            ReviewScreen(
                vm = viewModel { ReviewViewModel(container.repository, File(path)) },
                onDone = { nav.popBackStack(Routes.CAPTURE, inclusive = false) },
                onRetake = { nav.popBackStack(Routes.CAPTURE, inclusive = false) },
            )
        }
        composable(Routes.WRITE) {
            WriteScreen(
                vm = viewModel { WriteViewModel(container.repository) },
                onDone = { nav.popBackStack() },
                onClose = { nav.popBackStack() },
            )
        }
        composable(
            Routes.MONTH,
            // Swiping up from the camera pulls the journal up from below; going back drops it down again.
            enterTransition = { if (initialState.destination.route == Routes.CAPTURE) slideInVertically(sheet()) { it } else fadeIn() },
            popExitTransition = { if (targetState.destination.route == Routes.CAPTURE) slideOutVertically(sheet()) { it } else fadeOut() },
        ) {
            val status by container.settings.backupStatus.collectAsState(initial = null)
            val connected by container.backup.connected.collectAsState()
            MonthScreen(
                vm = monthVm,
                photoFile = container.repository::photoFile,
                banner = backupBanner(connected, status, System.currentTimeMillis()),
                onOpenDay = { nav.navigate(Routes.day(it)) },
                onCapture = { nav.popBackStack(Routes.CAPTURE, inclusive = false) },
                onWrite = { nav.navigate(Routes.WRITE) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onRestore = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.DAY, arguments = listOf(navArgument("date") { type = NavType.StringType })) { e ->
            val date = e.arguments?.getString("date").orEmpty()
            DayScreen(
                vm = viewModel { DayViewModel(container.repository, date) },
                photoFile = container.repository::photoFile,
                onBack = { nav.popBackStack() },
                onOpenEntry = { nav.navigate(Routes.entry(it)) },
                onCapture = { nav.popBackStack(Routes.CAPTURE, inclusive = false) },
            )
        }
        composable(Routes.ENTRY, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
            val id = e.arguments?.getString("id").orEmpty()
            EntryScreen(
                vm = viewModel { EntryViewModel(container.repository, id) },
                photoFile = container.repository::photoFile,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.IMPORT) {
            val context = LocalContext.current
            val uris = remember { container.pendingImport }
            ImportScreen(
                vm = viewModel { ImportViewModel(container.repository, open = { context.contentResolver.openInputStream(it) }) },
                uris = uris,
                onDone = {
                    container.pendingImport = emptyList()
                    nav.navigate(Routes.MONTH) { popUpTo(Routes.CAPTURE) }
                },
                onCancel = { container.pendingImport = emptyList(); nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                vm = viewModel {
                    SettingsViewModel(
                        container.repository, container.settings, container.context.filesDir,
                        onReminderChanged = { on, t -> container.applyReminder(on, t) },
                        backup = container.backupControls,
                    )
                },
                lockAvailable = remember { Biometrics.available(activity) },
                onBack = { nav.popBackStack() },
                onSetUpBackup = { nav.navigate(Routes.BACKUP_SETUP) },
                onRestore = { nav.navigate(Routes.RESTORE) },
                onShowPhrase = { verified -> Biometrics.prompt(activity, "Show recovery phrase", verified) },
            )
        }
        composable(Routes.BACKUP_SETUP) {
            BackupSetupScreen(
                vm = viewModel { BackupSetupViewModel(container.backup) },
                onDone = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.RESTORE) {
            RestoreScreen(
                vm = viewModel { RestoreViewModel(container.backup, container.db, container.photoStore) },
                onDone = { nav.navigate(Routes.MONTH) { popUpTo(Routes.CAPTURE) } },
                onBack = { nav.popBackStack() },
            )
        }
    }
}

/** Shown on the month view when backup has been failing for more than 3 days. */
fun backupBanner(connected: Boolean, status: BackupStatus?, now: Long): String? {
    val since = status?.failingSince ?: return null
    if (!connected || status.error == null) return null
    val days = (now - since) / 86_400_000L
    return if (days >= 3) "Backup has been failing for $days days. Tap to fix." else null
}
