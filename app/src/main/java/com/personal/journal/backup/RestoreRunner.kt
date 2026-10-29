package com.personal.journal.backup

import com.personal.journal.data.PhotoStore
import com.personal.journal.data.db.DateSource
import com.personal.journal.data.db.EntryEntity
import com.personal.journal.data.db.EntryType
import com.personal.journal.data.db.JournalDatabase
import com.personal.journal.data.normalizeTag
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.crypto.SecretKey

class NoBackupException : Exception("No journal backup was found in this bucket.")

data class RestoreProgress(val done: Int, val total: Int, val failed: Int = 0)

/**
 * Rebuilds the journal from the bucket. Entries merge by id (newer updatedAt wins), tombstones are
 * skipped. Restored rows count as backed up only when [markBackedUp] (restoring from this phone's own bucket).
 */
class RestoreRunner(
    private val db: JournalDatabase,
    private val photos: PhotoStore,
    private val store: ObjectStore,
    private val phrases: RecoveryPhrase,
    /** True only when restoring from the bucket this phone backs up to; otherwise restored rows still need uploading. */
    private val markBackedUp: Boolean = true,
) {
    /** Checks the phrase against the bucket's key check before anything else is downloaded. */
    suspend fun verify(words: List<String>): SecretKey {
        val key = deriveMasterKey(phrases.toEntropy(words))
        val meta = store.get(BackupRunner.META) ?: throw NoBackupException()
        if (!KeyCheck.matches(key, meta)) throw WrongPhraseException()
        return key
    }

    fun restore(key: SecretKey): Flow<RestoreProgress> = flow {
        val keys = store.list("entries/")
        var done = 0
        var failed = 0
        emit(RestoreProgress(0, keys.size))
        for (objectKey in keys) {
            val ok = runCatching { restoreOne(key, objectKey) }.getOrDefault(false)
            if (!ok) failed++
            done++
            emit(RestoreProgress(done, keys.size, failed))
        }
    }

    /** Returns false when the entry couldn't be restored (missing or unreadable photo). */
    private suspend fun restoreOne(key: SecretKey, objectKey: String): Boolean {
        val bytes = store.get(objectKey) ?: return false
        val e = BackupCodec.decode(Envelope.open(key, bytes, objectKey))
        if (e.deletedAt != null) return true
        val existing = db.entryDao().getById(e.id)?.entry
        if (existing != null && existing.updatedAt >= e.updatedAt) return true

        val type = EntryType.valueOf(e.type)
        val photoBytes = if (type == EntryType.PHOTO) {
            val photoKey = BackupCodec.photoKey(e.id)
            Envelope.open(key, store.get(photoKey) ?: return false, photoKey)
        } else null

        photos.writeLock.withLock {
            val stored = photoBytes?.let { photos.save(e.id, ByteArrayInputStream(it)) }
            db.withTransaction {
                db.entryDao().upsert(
                    EntryEntity(
                        id = e.id, type = type, text = e.text, photoFile = stored?.photoFile, thumbFile = stored?.thumbFile,
                        takenAt = e.takenAt, localDate = e.localDate, dateSource = DateSource.valueOf(e.dateSource),
                        createdAt = e.createdAt, updatedAt = e.updatedAt, deletedAt = null,
                        backedUpAt = if (markBackedUp) e.updatedAt else null,
                    ),
                )
                val slots = e.tagSlots.mapKeys { (name, _) -> normalizeTag(name) }
                val tagIds = e.tags.mapNotNull(::normalizeTag).distinct().map { name ->
                    db.tagDao().findByName(name)?.id
                        ?: db.tagDao().create(UUID.randomUUID().toString(), name, lastUsedAt = e.updatedAt, preferredSlot = slots[name]).id
                }
                db.tagDao().setTagsFor(e.id, tagIds)
            }
        }
        return true
    }
}
