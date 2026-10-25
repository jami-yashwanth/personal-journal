package com.personal.journal

import android.content.Context
import android.net.Uri
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.PhotoStore
import com.personal.journal.data.db.JournalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency wiring; one instance per process. */
class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db: JournalDatabase by lazy { JournalDatabase.create(context) }
    val photoStore: PhotoStore by lazy { PhotoStore(context.filesDir) }
    val repository: JournalRepository by lazy {
        JournalRepository(db, photoStore, onChanged = { onJournalChanged() })
    }

    /** Photos chosen in the system picker, handed from Capture to Import. */
    var pendingImport: List<Uri> = emptyList()

    /** Set by later wiring (backup scheduling). */
    var onJournalChanged: () -> Unit = {}
}
