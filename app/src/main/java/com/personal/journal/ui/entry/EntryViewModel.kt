package com.personal.journal.ui.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.db.EntryWithTags
import com.personal.journal.ui.components.TagPickerModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/** Pages through the day of entry [id], opening on it. Edit, date and delete act on the selected page. */
class EntryViewModel(private val repository: JournalRepository, private val id: String) : ViewModel() {
    private val _day = MutableStateFlow<List<EntryWithTags>>(emptyList())
    /** The day's entries in the Day screen's order (time taken, earliest first). */
    val day: StateFlow<List<EntryWithTags>> = _day

    private val _selected = MutableStateFlow(0)
    val selected: StateFlow<Int> = _selected

    private val _entry = MutableStateFlow<EntryWithTags?>(null)
    /** `day[selected]`, or null before [load] and once the day is empty. */
    val entry: StateFlow<EntryWithTags?> = _entry

    private val _dayIsEmpty = MutableStateFlow(false)
    /** True once loaded and nothing is left to show (the entry is gone or the last one was deleted). */
    val dayIsEmpty: StateFlow<Boolean> = _dayIsEmpty

    /** The opened entry's position in [day], resolved in [load]. */
    var initialIndex: Int = 0
        private set

    private var localDate: String? = null

    /** Non-null while editing caption/text and tags. */
    var editTags by mutableStateOf<TagPickerModel?>(null)
        private set

    private var loaded = false

    /** Resolves the day once; later calls (the screen re-entering composition) keep the current page. */
    suspend fun load() {
        if (loaded) return
        loaded = true
        showDayOf(id)
        initialIndex = _selected.value
    }

    fun select(index: Int) {
        val list = _day.value
        if (list.isEmpty()) return
        _selected.value = index.coerceIn(0, list.lastIndex)
        _entry.value = list[_selected.value]
    }

    fun startEdit() {
        val current = _entry.value ?: return
        editTags = TagPickerModel(repository, viewModelScope, current.tags.map { it.name }.toSet())
    }

    fun cancelEdit() {
        editTags = null
    }

    suspend fun saveEdit(text: String) {
        val tags = editTags ?: return
        val target = _entry.value?.entry?.id ?: return
        repository.update(target, text = text, tags = tags.selected.toList(), takenAt = null)
        editTags = null
        showDayOf(target)
    }

    /** The entry may move to another day; the pager follows it there. */
    suspend fun updateDate(millis: Long) {
        val target = _entry.value?.entry?.id ?: return
        repository.update(target, text = null, tags = null, takenAt = millis)
        showDayOf(target)
    }

    suspend fun delete() {
        val target = _entry.value?.entry?.id ?: return
        val index = _selected.value
        repository.delete(target)
        val date = localDate ?: return
        val list = repository.observeDay(date).first()
        publish(list, index.coerceAtMost(list.lastIndex))
    }

    /** Loads the day [entryId] is on, with it selected. */
    private suspend fun showDayOf(entryId: String) {
        val date = repository.get(entryId)?.entry?.localDate
        localDate = date
        val list = if (date == null) emptyList() else repository.observeDay(date).first()
        publish(list, list.indexOfFirst { it.entry.id == entryId })
    }

    private fun publish(list: List<EntryWithTags>, index: Int) {
        val i = index.coerceAtLeast(0)
        _day.value = list
        _selected.value = i
        _entry.value = list.getOrNull(i)
        _dayIsEmpty.value = list.isEmpty()
    }
}
