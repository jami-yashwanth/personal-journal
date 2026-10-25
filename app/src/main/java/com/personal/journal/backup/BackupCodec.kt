package com.personal.journal.backup

import com.personal.journal.data.db.EntryWithTags
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupEntry(
    val id: String,
    val type: String,
    val text: String? = null,
    val takenAt: Long,
    val localDate: String,
    val dateSource: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

object BackupCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun entryKey(id: String) = "entries/$id.json.enc"

    fun photoKey(id: String) = "photos/$id.jpg.enc"

    fun idFromEntryKey(key: String): String? =
        key.removePrefix("entries/").removeSuffix(".json.enc").takeIf { key.startsWith("entries/") && key.endsWith(".json.enc") }

    fun encode(e: EntryWithTags): ByteArray = json.encodeToString(
        BackupEntry.serializer(),
        BackupEntry(
            id = e.entry.id, type = e.entry.type.name, text = e.entry.text, takenAt = e.entry.takenAt,
            localDate = e.entry.localDate, dateSource = e.entry.dateSource.name, tags = e.tags.map { it.name }.sorted(),
            createdAt = e.entry.createdAt, updatedAt = e.entry.updatedAt, deletedAt = e.entry.deletedAt,
        ),
    ).toByteArray()

    fun decode(bytes: ByteArray): BackupEntry = json.decodeFromString(BackupEntry.serializer(), String(bytes))
}
