package com.personal.journal.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Upsert
    suspend fun upsert(entry: EntryEntity)

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: String): EntryWithTags?

    /** [prefix] is "YYYY-MM". Deleted entries are excluded. */
    @Transaction
    @Query("SELECT * FROM entries WHERE deletedAt IS NULL AND localDate LIKE :prefix || '-%' ORDER BY localDate DESC, takenAt ASC")
    fun observeMonth(prefix: String): Flow<List<EntryWithTags>>

    @Transaction
    @Query("SELECT * FROM entries WHERE deletedAt IS NULL AND localDate = :localDate ORDER BY takenAt ASC")
    fun observeDay(localDate: String): Flow<List<EntryWithTags>>

    @Query("SELECT * FROM entries WHERE backedUpAt IS NULL OR backedUpAt < updatedAt")
    suspend fun needsBackup(): List<EntryEntity>

    @Query("UPDATE entries SET backedUpAt = :at WHERE id = :id AND updatedAt <= :at")
    suspend fun markBackedUp(id: String, at: Long)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun purge(id: String)

    @Query("SELECT COUNT(*) FROM entries WHERE deletedAt IS NULL AND localDate = :localDate")
    suspend fun countOn(localDate: String): Int

    @Query("SELECT COUNT(*) FROM entries WHERE deletedAt IS NULL")
    fun observeLiveCount(): Flow<Int>

    @Query("SELECT id FROM entries")
    suspend fun allIds(): List<String>

    @Query("SELECT * FROM entries WHERE deletedAt IS NULL AND type = 'PHOTO'")
    suspend fun livePhotos(): List<EntryEntity>
}
