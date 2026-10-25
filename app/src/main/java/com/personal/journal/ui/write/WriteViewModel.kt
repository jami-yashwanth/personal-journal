package com.personal.journal.ui.write

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.journal.data.JournalRepository
import com.personal.journal.ui.components.TagPickerModel

class WriteViewModel(private val repository: JournalRepository) : ViewModel() {
    val tags = TagPickerModel(repository, viewModelScope)

    var text by mutableStateOf("")
        private set
    var takenAt by mutableLongStateOf(repository.now())
        private set

    val canSave: Boolean get() = text.isNotBlank()
    val hasUnsavedText: Boolean get() = text.isNotBlank()

    fun updateText(value: String) {
        text = value
    }

    fun updateTakenAt(millis: Long) {
        takenAt = millis
    }

    /** Returns the new entry id, or null when there is nothing to save. */
    suspend fun save(): String? {
        if (!canSave) return null
        val id = repository.saveText(text, tags.selected.toList(), takenAt)
        text = ""
        return id
    }
}
