package com.personal.journal.ui.day

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.db.EntryWithTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** One moment on the day page; [aspect] is the photo's width / height, null for a note or an unreadable photo. */
data class DayItem(val entry: EntryWithTags, val aspect: Float?)

data class DayUiState(val title: String, val subtitle: String, val items: List<DayItem>)

/** A photo tile keeps the shot's shape, held between 3:5 and 5:3 so one panorama can't take over the page. */
fun photoAspect(width: Int, height: Int): Float? =
    if (width <= 0 || height <= 0) null else (width.toFloat() / height).coerceIn(3f / 5f, 5f / 3f)

/** Reads only the thumbnail's header, so it is cheap enough to do for a whole day. */
private fun aspectOf(file: File): Float? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    return photoAspect(bounds.outWidth, bounds.outHeight)
}

class DayViewModel(repository: JournalRepository, localDate: String) : ViewModel() {
    private val date = LocalDate.parse(localDate)
    private val isToday = localDate == repository.today()

    val state: StateFlow<DayUiState?> = repository.observeDay(localDate).map { list ->
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val month = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val n = list.size
        DayUiState(
            title = "$weekday, ${date.dayOfMonth}",
            subtitle = "${if (isToday) "Today" else "$month ${date.year}"}, $n ${if (n == 1) "entry" else "entries"}",
            items = list.map { e -> DayItem(e, e.entry.thumbFile?.let { aspectOf(repository.photoFile(it)) }) },
        )
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
