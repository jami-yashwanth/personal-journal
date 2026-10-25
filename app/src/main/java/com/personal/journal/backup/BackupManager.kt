package com.personal.journal.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.personal.journal.JournalApp
import com.personal.journal.data.AppSettings
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.PhotoStore
import com.personal.journal.data.db.JournalDatabase
import com.personal.journal.ui.settings.BackupControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

class WrongPhraseException : Exception("Recovery phrase doesn't match this backup.")

/** Owns backup setup, scheduling and status; the settings screen talks to it through [BackupControls]. */
class BackupManager(
    private val context: Context,
    private val db: JournalDatabase,
    private val photos: PhotoStore,
    private val repository: JournalRepository,
    private val settings: AppSettings,
    val credentials: CredentialStore = CredentialStore(context),
) : BackupControls {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .build()
    val phrases: RecoveryPhrase by lazy {
        RecoveryPhrase(context.resources.openRawResource(com.personal.journal.R.raw.bip39_english).bufferedReader().readLines().filter { it.isNotBlank() })
    }
    private val _connected = MutableStateFlow(credentials.isConnected())
    override val connected: StateFlow<Boolean> = _connected

    @Volatile private var useMobileData = false

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settings.backupUseMobileData.collect { on ->
                val changed = on != useMobileData
                useMobileData = on
                if (changed && _connected.value) schedulePeriodic()
            }
        }
    }

    fun client(c: R2Credentials): ObjectStore = R2Client(http, c)

    /** Writes, reads and deletes a probe object. Returns true when the bucket already holds a journal backup. */
    suspend fun testConnection(c: R2Credentials): Boolean {
        val store = client(c)
        val probe = "probe-${System.currentTimeMillis()}"
        store.put(probe, byteArrayOf(1))
        if (store.get(probe) == null) throw BackupException(BackupErrorKind.UNKNOWN)
        store.delete(probe)
        return store.get(BackupRunner.META) != null
    }

    fun newPhrase(): List<String> = phrases.generate(SecureRandom())

    /** Verifies [words] against the bucket's key check (when one exists) and returns the master key. */
    suspend fun keyFor(c: R2Credentials, words: List<String>): SecretKey {
        val key = deriveMasterKey(phrases.toEntropy(words))
        val meta = client(c).get(BackupRunner.META)
        if (meta != null && !KeyCheck.matches(key, meta)) throw WrongPhraseException()
        return key
    }

    /** Connecting (to any bucket) re-uploads the whole journal: rows marked backed up may have gone to a different bucket. */
    suspend fun connect(c: R2Credentials, words: List<String>) {
        val key = keyFor(c, words)
        db.entryDao().resetBackup()
        credentials.saveR2(c)
        credentials.saveMasterKey(key)
        credentials.savePhrase(words)
        _connected.value = true
        schedulePeriodic()
        backUpNow()
    }

    override fun backUpNow() = enqueue(delayMinutes = 0)

    fun onJournalChanged() {
        if (_connected.value) enqueue(delayMinutes = 2)
    }

    override suspend fun disconnect() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_SOON)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_PERIODIC)
        credentials.clear()
        settings.clearBackupStatus()
        _connected.value = false
    }

    override fun recoveryPhrase(): List<String>? = credentials.loadPhrase()

    private fun constraints(mobileData: Boolean) = Constraints.Builder()
        .setRequiredNetworkType(if (mobileData) NetworkType.CONNECTED else NetworkType.UNMETERED)
        .setRequiresBatteryNotLow(true)
        .build()

    private fun enqueue(delayMinutes: Long) {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints(useMobileData))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_SOON, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(6, TimeUnit.HOURS).setConstraints(constraints(useMobileData)).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** One backup pass with status bookkeeping. Returns true when it should be retried. */
    suspend fun runPass(): Boolean = passLock.withLock { runPassLocked() }

    /** True when [c] is the bucket this phone currently backs up to. */
    fun isCurrentBucket(c: R2Credentials): Boolean =
        _connected.value && credentials.loadR2()?.let { it.accountId == c.accountId && it.bucket == c.bucket } == true

    private suspend fun runPassLocked(): Boolean {
        val c = credentials.loadR2() ?: return false
        val key = credentials.loadMasterKey() ?: return false
        val report = BackupRunner(db, photos, client(c), key).runOnce()
        val now = System.currentTimeMillis()
        if (report.failed == 0 && report.lastError == null) {
            settings.recordBackupSuccess(now, repository.observeLiveCount().first())
            return false
        }
        val kind = report.lastError ?: BackupErrorKind.UNKNOWN
        settings.recordBackupFailure(now, kind.message)
        return kind == BackupErrorKind.NETWORK
    }

    companion object {
        /** One backup pass at a time per process; overlapping passes could re-upload a just-deleted entry. */
        private val passLock = Mutex()
        const val WORK_SOON = "backup-soon"
        const val WORK_PERIODIC = "backup-periodic"
    }
}

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val manager = (applicationContext as JournalApp).container.backup
        return if (manager.runPass()) Result.retry() else Result.success()
    }
}
