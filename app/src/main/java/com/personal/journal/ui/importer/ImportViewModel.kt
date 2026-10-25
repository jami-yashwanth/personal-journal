package com.personal.journal.ui.importer

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.db.DateSource
import com.personal.journal.data.exifDateToMillis
import com.personal.journal.ui.components.TagPickerModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.ZoneId

data class ImportItem(
    val uri: Uri,
    val takenAt: Long,
    val hasExifDate: Boolean,
    val dateEdited: Boolean = false,
    val caption: String = "",
    val error: String? = null,
)

data class ImportResult(val saved: Int, val failed: List<Int>)

class ImportViewModel(
    private val repository: JournalRepository,
    private val open: (Uri) -> InputStream?,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {
    val tags = TagPickerModel(repository, viewModelScope)
    private val _items = MutableStateFlow<List<ImportItem>>(emptyList())
    val items: StateFlow<List<ImportItem>> = _items

    suspend fun load(uris: List<Uri>) {
        val now = repository.now()
        _items.value = withContext(Dispatchers.IO) { uris.map { inspect(it, now) } }
    }

    private fun inspect(uri: Uri, now: Long): ImportItem {
        val readable = runCatching {
            open(uri)?.use { s -> BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeStream(s, null, it) }.outWidth > 0 }
        }.getOrNull() == true
        if (!readable) return ImportItem(uri, now, hasExifDate = false, error = "This photo couldn't be read")
        val exif = runCatching { open(uri)?.use { ExifInterface(it).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) } }.getOrNull()
        val millis = exifDateToMillis(exif, zone())
        return ImportItem(uri, millis ?: now, hasExifDate = millis != null)
    }

    fun setCaption(index: Int, caption: String) = edit(index) { it.copy(caption = caption) }

    fun setDate(index: Int, millis: Long) = edit(index) { it.copy(takenAt = millis, dateEdited = true) }

    private fun edit(index: Int, change: (ImportItem) -> ImportItem) =
        _items.update { list -> list.mapIndexed { i, item -> if (i == index) change(item) else item } }

    /** Saves every readable photo; failures are reported by index and don't stop the others. */
    suspend fun saveAll(): ImportResult {
        var saved = 0
        val failed = mutableListOf<Int>()
        val shared = tags.selected.toList()
        _items.value.forEachIndexed { i, item ->
            if (item.error != null) {
                failed += i
                return@forEachIndexed
            }
            val source = when {
                item.dateEdited -> DateSource.MANUAL
                item.hasExifDate -> DateSource.EXIF
                else -> DateSource.FALLBACK_TODAY
            }
            val ok = runCatching {
                open(item.uri)!!.use { repository.savePhoto(it, item.caption, shared, item.takenAt, source) }
            }.isSuccess
            if (ok) saved++ else failed += i
        }
        return ImportResult(saved, failed)
    }
}
