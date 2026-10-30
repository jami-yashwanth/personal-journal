package com.personal.journal.data

import androidx.room.withTransaction
import com.personal.journal.data.db.DateSource
import com.personal.journal.data.db.EntryEntity
import com.personal.journal.data.db.EntryType
import com.personal.journal.data.db.EntryWithTags
import com.personal.journal.data.db.JournalDatabase
import com.personal.journal.data.db.TagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.io.InputStream
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class InsufficientStorageException : IOException("Not enough free space to save this photo")

/** The only writer of the journal database and photo files. */
class JournalRepository(
    private val db: JournalDatabase,
    private val photos: PhotoStore,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val onChanged: () -> Unit = {},
    private val freeBytes: () -> Long = { photos.freeBytes() },
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val entries = db.entryDao()
    private val tags = db.tagDao()

    fun now(): Long = clock.millis()

    /** updatedAt doubles as a version for backup, so it must strictly increase even within one millisecond. */
    private fun nextVersion(previous: Long) = maxOf(now(), previous + 1)

    fun today(): String = localDateOf(now(), zone())

    fun isToday(millis: Long): Boolean = localDateOf(millis, zone()) == today()

    suspend fun savePhoto(
        source: InputStream,
        caption: String?,
        tags: List<String>,
        takenAt: Long?,
        dateSource: DateSource,
    ): String {
        if (freeBytes() < MIN_FREE_BYTES) throw InsufficientStorageException()
        val id = newId()
        // Held across file write + row insert so startup cleanup never sees a file without its row.
        photos.writeLock.withLock {
            val stored = photos.save(id, source)
            val (time, source) = when {
                takenAt != null -> takenAt to dateSource
                else -> exifDateToMillis(stored.exifTakenAt, zone())?.let { it to DateSource.EXIF }
                    ?: (now() to DateSource.FALLBACK_TODAY)
            }
            try {
                insert(id, EntryType.PHOTO, caption?.trim()?.ifEmpty { null }, stored.photoFile, stored.thumbFile, time, source, tags)
            } catch (e: Exception) {
                photos.delete(id)
                throw e
            }
        }
        return id
    }

    suspend fun saveText(text: String, tags: List<String>, takenAt: Long): String {
        val body = text.trim()
        require(body.isNotEmpty()) { "Text entry is empty" }
        val id = newId()
        insert(id, EntryType.TEXT, body, null, null, takenAt, DateSource.MANUAL, tags)
        return id
    }

    private suspend fun insert(
        id: String, type: EntryType, text: String?, photo: String?, thumb: String?,
        takenAt: Long, source: DateSource, tagNames: List<String>,
    ) {
        val t = now()
        db.withTransaction {
            entries.upsert(
                EntryEntity(
                    id = id, type = type, text = text, photoFile = photo, thumbFile = thumb,
                    takenAt = takenAt, localDate = localDateOf(takenAt, zone()), dateSource = source,
                    createdAt = t, updatedAt = t, deletedAt = null, backedUpAt = null,
                ),
            )
            applyTags(id, tagNames, previous = emptySet())
        }
        onChanged()
    }

    /** Null arguments leave that field unchanged. */
    suspend fun update(id: String, text: String?, tags: List<String>?, takenAt: Long?) {
        db.withTransaction {
            val current = entries.getById(id) ?: return@withTransaction
            var e = current.entry.copy(updatedAt = nextVersion(current.entry.updatedAt))
            if (text != null) e = e.copy(text = text.trim().ifEmpty { null })
            if (takenAt != null) {
                e = e.copy(takenAt = takenAt, localDate = localDateOf(takenAt, zone()), dateSource = DateSource.MANUAL)
            }
            entries.upsert(e)
            if (tags != null) applyTags(id, tags, previous = current.tags.map { it.name }.toSet())
        }
        onChanged()
    }

    /**
     * Always leaves a content-free tombstone (never a hard purge): an upload of this entry may already be
     * in flight or done-but-unmarked, and only a tombstone lets backup remove it from the bucket.
     * Backup purges the row once the tombstone is uploaded.
     */
    suspend fun delete(id: String) {
        val current = entries.getById(id)?.entry ?: return
        photos.delete(id)
        db.withTransaction {
            entries.upsert(
                current.copy(
                    text = null, photoFile = null, thumbFile = null,
                    deletedAt = now(), updatedAt = nextVersion(current.updatedAt),
                ),
            )
            tags.clearTagsFor(id)
        }
        onChanged()
    }

    private suspend fun applyTags(entryId: String, names: List<String>, previous: Set<String>) {
        val t = now()
        val normalized = names.mapNotNull(::normalizeTag).distinct()
        val ids = normalized.map { name ->
            val existing = tags.findByName(name)
            when {
                existing == null -> TagEntity(newId(), name, useCount = 1, lastUsedAt = t).also { tags.insert(it) }
                name !in previous -> existing.copy(useCount = existing.useCount + 1, lastUsedAt = t).also { tags.update(it) }
                else -> existing
            }.id
        }
        tags.setTagsFor(entryId, ids)
    }

    fun observeMonth(month: YearMonth): Flow<List<EntryWithTags>> = entries.observeMonth(month.toString())

    fun observeDay(localDate: String): Flow<List<EntryWithTags>> = entries.observeDay(localDate)

    fun observeLiveCount(): Flow<Int> = entries.observeLiveCount()

    suspend fun get(id: String): EntryWithTags? = entries.getById(id)?.takeIf { it.entry.deletedAt == null }

    suspend fun countOn(localDate: String): Int = entries.countOn(localDate)

    suspend fun pickerTags(selected: Set<String>): List<String> = tags.orderedForPicker(selected.toList()).map { it.name }

    suspend fun searchTags(query: String): List<String> =
        normalizeTag(query)?.let { q -> tags.search(q).map { it.name } } ?: emptyList()

    suspend fun monthTags(month: YearMonth): List<String> = tags.tagsUsedInMonth(month.toString()).map { it.name }

    /** Removes photo files that no entry refers to, plus leftover raw camera captures in [cacheDir]. */
    suspend fun startupCleanup(cacheDir: java.io.File? = null): Int = photos.writeLock.withLock {
        val captures = cacheDir?.listFiles { f -> f.name.startsWith("capture-") }?.count { it.delete() } ?: 0
        photos.cleanup(entries.allIds().toSet()) + captures
    }

    fun photoFile(relative: String) = photos.file(relative)

    companion object {
        const val MIN_FREE_BYTES = 50L * 1024 * 1024
    }
}
