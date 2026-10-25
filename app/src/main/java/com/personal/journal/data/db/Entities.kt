package com.personal.journal.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class EntryType { PHOTO, TEXT }

enum class DateSource { CAMERA, EXIF, FALLBACK_TODAY, MANUAL }

@Entity(
    tableName = "entries",
    indices = [Index("localDate"), Index("takenAt"), Index("deletedAt", "backedUpAt")],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val type: EntryType,
    val text: String?,
    val photoFile: String?,
    val thumbFile: String?,
    val takenAt: Long,
    val localDate: String,
    val dateSource: DateSource,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val backedUpAt: Long?,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val useCount: Int,
    val lastUsedAt: Long,
)

@Entity(
    tableName = "entry_tags",
    primaryKeys = ["entryId", "tagId"],
    indices = [Index("tagId")],
    foreignKeys = [
        ForeignKey(EntryEntity::class, ["id"], ["entryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TagEntity::class, ["id"], ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class EntryTagCrossRef(val entryId: String, val tagId: String)

data class EntryWithTags(
    @Embedded val entry: EntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(EntryTagCrossRef::class, parentColumn = "entryId", entityColumn = "tagId"),
    )
    val tags: List<TagEntity>,
)
