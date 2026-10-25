package com.personal.journal.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.journal.data.JournalRepository
import com.personal.journal.data.db.DateSource
import com.personal.journal.ui.components.TagPickerModel
import java.io.File

class ReviewViewModel(private val repository: JournalRepository, val photo: File) : ViewModel() {
    val tags = TagPickerModel(repository, viewModelScope)
    val takenAt: Long = repository.now()

    suspend fun save(caption: String): String {
        val id = photo.inputStream().use {
            repository.savePhoto(it, caption, tags.selected.toList(), takenAt, DateSource.CAMERA)
        }
        photo.delete()
        return id
    }

    fun discard() {
        photo.delete()
    }
}
