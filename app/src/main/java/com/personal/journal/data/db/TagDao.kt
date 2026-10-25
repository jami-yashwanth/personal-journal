package com.personal.journal.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: TagEntity)

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM entry_tags WHERE entryId = :entryId")
    suspend fun clearTagsFor(entryId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRefs(refs: List<EntryTagCrossRef>)

    @Transaction
    suspend fun setTagsFor(entryId: String, tagIds: List<String>) {
        clearTagsFor(entryId)
        insertRefs(tagIds.map { EntryTagCrossRef(entryId, it) })
    }

    /** Selected names first, then most used, then most recently used. */
    @Query(
        "SELECT * FROM tags ORDER BY CASE WHEN name IN (:selected) THEN 0 ELSE 1 END, useCount DESC, lastUsedAt DESC, name ASC",
    )
    suspend fun orderedForPicker(selected: List<String>): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END, useCount DESC, name ASC")
    suspend fun search(query: String): List<TagEntity>

    @Query(
        """SELECT DISTINCT t.* FROM tags t
           JOIN entry_tags et ON et.tagId = t.id
           JOIN entries e ON e.id = et.entryId
           WHERE e.deletedAt IS NULL AND e.localDate LIKE :prefix || '-%'
           ORDER BY t.useCount DESC, t.name ASC""",
    )
    suspend fun tagsUsedInMonth(prefix: String): List<TagEntity>
}
