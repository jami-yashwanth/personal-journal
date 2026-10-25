package com.personal.journal.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.journal.data.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModel(private val repository: JournalRepository) : ViewModel() {
    private val today = MutableStateFlow(repository.today())

    val todayCount: StateFlow<Int> = today
        .flatMapLatest { repository.observeDay(it) }
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Called on resume so the count rolls over at midnight. */
    fun refreshDay() {
        today.value = repository.today()
    }
}
