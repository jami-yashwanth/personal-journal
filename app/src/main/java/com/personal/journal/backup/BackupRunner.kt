package com.personal.journal.backup

import com.personal.journal.data.PhotoStore
import com.personal.journal.data.db.EntryType
import com.personal.journal.data.db.JournalDatabase
import javax.crypto.SecretKey

data class BackupReport(val uploaded: Int, val deleted: Int, val failed: Int, val lastError: BackupErrorKind?)

/**
 * One incremental backup pass. Per entry the photo goes up first, then the entry JSON, and only then
 * is the row marked backed up, so an interrupted pass simply retries that whole entry next time.
 */
class BackupRunner(
    private val db: JournalDatabase,
    private val photos: PhotoStore,
    private val store: ObjectStore,
    private val key: SecretKey,
) {
    private val entries = db.entryDao()

    suspend fun runOnce(): BackupReport {
        try {
            ensureMeta()
        } catch (e: BackupException) {
            return BackupReport(0, 0, entries.needsBackup().size, e.kind)
        }
        var uploaded = 0
        var deleted = 0
        var failed = 0
        var lastError: BackupErrorKind? = null
        for (row in entries.needsBackup()) {
            try {
                val full = entries.getById(row.id) ?: continue
                val entryKey = BackupCodec.entryKey(row.id)
                if (row.deletedAt != null) {
                    store.put(entryKey, Envelope.seal(key, BackupCodec.encode(full), entryKey))
                    store.delete(BackupCodec.photoKey(row.id))
                    entries.purge(row.id)
                    deleted++
                } else {
                    if (row.type == EntryType.PHOTO && row.photoFile != null) {
                        val photoKey = BackupCodec.photoKey(row.id)
                        store.put(photoKey, Envelope.seal(key, photos.file(row.photoFile).readBytes(), photoKey))
                    }
                    store.put(entryKey, Envelope.seal(key, BackupCodec.encode(full), entryKey))
                    entries.markBackedUp(row.id, row.updatedAt)
                    uploaded++
                }
            } catch (e: BackupException) {
                failed++
                lastError = e.kind
            } catch (e: java.io.IOException) {
                failed++
                lastError = BackupErrorKind.UNKNOWN
            }
        }
        return BackupReport(uploaded, deleted, failed, lastError)
    }

    /** Writes the key check on first use; refuses a bucket that holds someone else's (or another phrase's) backup. */
    private suspend fun ensureMeta() {
        val existing = store.get(META)
        if (existing == null) store.put(META, KeyCheck.create(key))
        else if (!KeyCheck.matches(key, existing)) throw BackupException(BackupErrorKind.WRONG_KEY)
    }

    companion object {
        const val META = "meta.json.enc"
    }
}
