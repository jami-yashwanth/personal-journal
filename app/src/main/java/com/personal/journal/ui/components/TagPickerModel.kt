package com.personal.journal.ui.components

import com.personal.journal.data.JournalRepository
import com.personal.journal.data.normalizeTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared tag-row logic for screens that pick tags (Review, Write, Import, Entry edit). */
class TagPickerModel(
    private val repository: JournalRepository,
    private val scope: CoroutineScope,
    initial: Set<String> = emptySet(),
) {
    private val _state = MutableStateFlow(TagRowState(selected = initial))
    val state: StateFlow<TagRowState> = _state

    val selected: Set<String> get() = _state.value.selected

    init {
        refresh()
    }

    private fun refresh() = scope.launch {
        val tags = repository.pickerTags(_state.value.selected)
        _state.update { it.copy(tags = (tags + it.selected).distinct()) }
    }

    fun toggle(tag: String) {
        _state.update { s -> s.copy(selected = if (tag in s.selected) s.selected - tag else s.selected + tag) }
    }

    fun openSearch() = _state.update { it.copy(searching = true, query = "", results = emptyList(), canCreate = false) }

    fun closeSearch() {
        _state.update { it.copy(searching = false, query = "") }
        refresh()
    }

    fun query(text: String) {
        _state.update { it.copy(query = text) }
        scope.launch {
            val results = repository.searchTags(text)
            _state.update { if (it.query == text) it.copy(results = results, canCreate = canCreateTag(text, results)) else it }
        }
    }

    /** New tags exist only in the selection until the entry is saved. */
    fun create(text: String) {
        val name = normalizeTag(text) ?: return
        _state.update { it.copy(selected = it.selected + name, tags = (listOf(name) + it.tags).distinct(), query = "", searching = false) }
    }
}
