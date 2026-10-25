package com.personal.journal.ui.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.journal.data.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MonthViewModel(private val repository: JournalRepository) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(LocalDate.parse(repository.today())))
    private val activeTag = MutableStateFlow<String?>(null)

    val state: StateFlow<MonthUiState?> = month
        .flatMapLatest { m -> combine(repository.observeMonth(m), activeTag) { entries, tag -> Triple(m, entries, tag) } }
        .mapLatest { (m, entries, tag) ->
            val tags = repository.monthTags(m)
            buildMonthUi(m, entries, repository.today(), tag?.takeIf { it in tags }, tags)
        }
        // Loaded up front and kept current, so the journal has content on its first frame when it slides in.
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setTag(tag: String?) {
        activeTag.value = tag
    }

    fun showMonth(m: YearMonth) {
        if (m.isAfter(YearMonth.from(LocalDate.parse(repository.today())))) return
        activeTag.value = null
        month.value = m
    }

    /** Back to this month with no tag filter, as when the journal is opened from the camera. */
    fun showCurrentMonth() = showMonth(YearMonth.from(LocalDate.parse(repository.today())))

    fun previous() = showMonth(month.value.minusMonths(1))

    fun next() = showMonth(month.value.plusMonths(1))
}
