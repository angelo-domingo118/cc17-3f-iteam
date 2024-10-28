package com.example.neuralnotesproject.viewmodels

import androidx.lifecycle.*
import com.example.neuralnotesproject.data.Source
import com.example.neuralnotesproject.repository.SourceRepository
import kotlinx.coroutines.launch

class SourceViewModel(private val repository: SourceRepository, private val notebookId: String) : ViewModel() {
    val sources: LiveData<List<Source>> = repository.getSourcesForNotebook(notebookId)

    fun addSource(source: Source) = viewModelScope.launch {
        repository.insertSource(source)
    }

    fun updateSource(source: Source) = viewModelScope.launch {
        repository.updateSource(source)
    }

    fun deleteSource(source: Source) = viewModelScope.launch {
        repository.deleteSource(source)
    }
}
