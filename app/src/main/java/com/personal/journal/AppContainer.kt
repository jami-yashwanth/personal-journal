package com.personal.journal

import android.content.Context
import android.net.Uri
import com.personal.journal.data.AppSettings
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.PhotoStore
import com.personal.journal.data.db.JournalDatabase
import com.personal.journal.reminder.ReminderScheduler
import com.personal.journal.ui.lock.LockGate
import com.personal.journal.ui.settings.BackupControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

/** Manual dependency wiring; one instance per process. */
class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db: JournalDatabase by lazy { JournalDatabase.create(context) }
    val photoStore: PhotoStore by lazy { PhotoStore(context.filesDir) }
    val settings: AppSettings by lazy { AppSettings(context) }
    val lockGate = LockGate()
    val repository: JournalRepository by lazy {
        JournalRepository(db, photoStore, onChanged = { onJournalChanged() })
    }

    /** Photos chosen in the system picker, handed from Capture to Import. */
    var pendingImport: List<Uri> = emptyList()

    /** Replaced by backup wiring. */
    var onJournalChanged: () -> Unit = {}
    var backupControls: BackupControls = object : BackupControls {
        override val connected: StateFlow<Boolean> = MutableStateFlow(false)
        override fun backUpNow() = Unit
        override suspend fun disconnect() = Unit
        override fun recoveryPhrase(): List<String>? = null
    }

    fun applyReminder(enabled: Boolean, time: LocalTime, replace: Boolean = true) {
        if (enabled) ReminderScheduler.schedule(context, time, replace) else ReminderScheduler.cancel(context)
    }
}
