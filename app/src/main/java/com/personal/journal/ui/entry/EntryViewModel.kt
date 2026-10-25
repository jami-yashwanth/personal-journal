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

class EntryViewModel(private val repository: JournalRepository, private val id: String) : ViewModel() {
    private val _entry = MutableStateFlow<EntryWithTags?>(null)
    val entry: StateFlow<EntryWithTags?> = _entry

    /** Non-null while editing caption/text and tags. */
    var editTags by mutableStateOf<TagPickerModel?>(null)
        private set

    suspend fun load() {
        _entry.value = repository.get(id)
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
        repository.update(id, text = text, tags = tags.selected.toList(), takenAt = null)
        editTags = null
        load()
    }

    suspend fun updateDate(millis: Long) {
        repository.update(id, text = null, tags = null, takenAt = millis)
        load()
    }

    suspend fun delete() {
        repository.delete(id)
    }
}
