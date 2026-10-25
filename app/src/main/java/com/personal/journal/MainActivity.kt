package com.personal.journal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.personal.journal.ui.capture.CaptureViewModel
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
        setContent {
            JournalTheme {
                val nav = rememberNavController()
                LaunchedEffect(shortcut) {
                    if (shortcut == ACTION_WRITE) nav.navigate(Routes.WRITE) { launchSingleTop = true }
                    shortcut = null
                }
                JournalNavHost(nav)
            }
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

@Composable
private fun JournalNavHost(nav: NavHostController) {
    val container = appContainer()
    NavHost(
        nav,
        startDestination = Routes.CAPTURE,
        modifier = Modifier.fillMaxSize().background(JournalColors.Background),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(Routes.CAPTURE) {
            CaptureScreen(
                vm = viewModel { CaptureViewModel(container.repository) },
                onOpenMonth = { nav.navigate(Routes.MONTH) },
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
        composable(Routes.MONTH) {
            MonthScreen(
                vm = viewModel { MonthViewModel(container.repository) },
                photoFile = container.repository::photoFile,
                banner = null,
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
        composable(Routes.SETTINGS) { Box(Modifier.fillMaxSize()) }
    }
}
