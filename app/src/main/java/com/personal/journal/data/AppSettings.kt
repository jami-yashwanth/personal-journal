package com.personal.journal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("settings")

data class BackupStatus(
    val lastSuccessAt: Long?,
    val entryCount: Int,
    val error: String?,
    val failingSince: Long?,
)

class AppSettings(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.settingsStore)

    private object K {
        val lock = booleanPreferencesKey("lock")
        val reminder = booleanPreferencesKey("reminder")
        val reminderTime = stringPreferencesKey("reminder_time")
        val mobileData = booleanPreferencesKey("backup_mobile_data")
        val askedNotifications = booleanPreferencesKey("asked_notifications")
        val lastSuccess = longPreferencesKey("backup_last_success")
        val count = intPreferencesKey("backup_count")
        val error = stringPreferencesKey("backup_error")
        val failingSince = longPreferencesKey("backup_failing_since")
    }

    val lockEnabled: Flow<Boolean> = store.data.map { it[K.lock] ?: true }
    val reminderEnabled: Flow<Boolean> = store.data.map { it[K.reminder] ?: true }
    val reminderTime: Flow<LocalTime> = store.data.map { p -> p[K.reminderTime]?.let(LocalTime::parse) ?: LocalTime.of(21, 0) }
    val backupUseMobileData: Flow<Boolean> = store.data.map { it[K.mobileData] ?: false }
    val backupStatus: Flow<BackupStatus> = store.data.map {
        BackupStatus(it[K.lastSuccess], it[K.count] ?: 0, it[K.error], it[K.failingSince])
    }

    suspend fun setLock(on: Boolean) = store.edit { it[K.lock] = on }
    suspend fun setReminder(on: Boolean) = store.edit { it[K.reminder] = on }
    suspend fun setReminderTime(t: LocalTime) = store.edit { it[K.reminderTime] = t.toString() }
    suspend fun setBackupUseMobileData(on: Boolean) = store.edit { it[K.mobileData] = on }

    /** True only the first time it's called. */
    suspend fun firstNotificationAsk(): Boolean {
        val asked = store.data.first()[K.askedNotifications] ?: false
        if (!asked) store.edit { it[K.askedNotifications] = true }
        return !asked
    }

    suspend fun recordBackupSuccess(at: Long, entries: Int) = store.edit {
        it[K.lastSuccess] = at
        it[K.count] = entries
        it.remove(K.error)
        it.remove(K.failingSince)
    }

    suspend fun recordBackupFailure(at: Long, message: String) = store.edit {
        it[K.error] = message
        if (it[K.failingSince] == null) it[K.failingSince] = at
    }

    suspend fun clearBackupStatus() = store.edit {
        it.remove(K.lastSuccess); it.remove(K.count); it.remove(K.error); it.remove(K.failingSince)
    }
}
