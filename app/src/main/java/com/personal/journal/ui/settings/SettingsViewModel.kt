package com.personal.journal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.journal.data.AppSettings
import com.personal.journal.data.BackupStatus
import com.personal.journal.data.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalTime

data class SettingsUi(
    val lock: Boolean,
    val reminder: Boolean,
    val reminderTime: LocalTime,
    val backupConnected: Boolean,
    val backup: BackupStatus,
    val useMobileData: Boolean,
    val storageLine: String,
    val entryCount: Int,
)

/** Backup actions the settings screen needs; implemented by the backup package. */
interface BackupControls {
    val connected: StateFlow<Boolean>
    fun backUpNow()
    suspend fun disconnect()
    fun recoveryPhrase(): List<String>?
}

class SettingsViewModel(
    private val repository: JournalRepository,
    private val settings: AppSettings,
    private val filesDir: File,
    private val onReminderChanged: (enabled: Boolean, time: LocalTime) -> Unit,
    val backup: BackupControls,
) : ViewModel() {
    private val storage = MutableStateFlow("")

    init {
        viewModelScope.launch { refreshStorage() }
    }

    private suspend fun refreshStorage() {
        val count = repository.observeLiveCount().first()
        val bytes = withContext(Dispatchers.IO) {
            File(filesDir, "photos").walkTopDown().filter { it.isFile }.sumOf { it.length() } +
                File(filesDir, "thumbs").walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        val mb = bytes / (1024.0 * 1024.0)
        val size = if (mb < 10) "%.1f".format(mb) else "%.0f".format(mb)
        storage.value = "$count ${if (count == 1) "entry" else "entries"}, $size MB on this phone"
    }

    val state: StateFlow<SettingsUi?> = combine(
        combine(settings.lockEnabled, settings.reminderEnabled, settings.reminderTime) { a, b, c -> Triple(a, b, c) },
        combine(backup.connected, settings.backupStatus, settings.backupUseMobileData) { a, b, c -> Triple(a, b, c) },
        combine(storage, repository.observeLiveCount()) { line, count -> line to count },
    ) { (lock, reminder, time), (connected, status, mobile), (storageLine, count) ->
        SettingsUi(lock, reminder, time, connected, status, mobile, storageLine, count)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setLock(on: Boolean) = viewModelScope.launch { settings.setLock(on) }

    fun setReminder(on: Boolean) = viewModelScope.launch {
        settings.setReminder(on)
        onReminderChanged(on, settings.reminderTime.first())
    }

    fun setReminderTime(t: LocalTime) = viewModelScope.launch {
        settings.setReminderTime(t)
        onReminderChanged(settings.reminderEnabled.first(), t)
    }

    fun setUseMobileData(on: Boolean) = viewModelScope.launch { settings.setBackupUseMobileData(on) }
}
