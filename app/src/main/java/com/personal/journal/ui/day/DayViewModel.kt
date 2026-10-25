package com.personal.journal.ui.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.db.EntryWithTags
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DayUiState(val title: String, val subtitle: String, val entries: List<EntryWithTags>)

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
            entries = list,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
