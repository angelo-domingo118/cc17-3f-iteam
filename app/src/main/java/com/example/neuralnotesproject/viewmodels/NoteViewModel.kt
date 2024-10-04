package com.example.neuralnotesproject.viewmodels

import androidx.lifecycle.*
import com.example.neuralnotesproject.data.Note
import com.example.neuralnotesproject.repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository, private val notebookId: String) : ViewModel() {
    val notes: LiveData<List<Note>> = repository.getNotesForNotebook(notebookId)

    fun addNote(note: Note) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repository.updateNote(note)
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
    }
}

class NoteViewModelFactory(private val repository: NoteRepository, private val notebookId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository, notebookId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}