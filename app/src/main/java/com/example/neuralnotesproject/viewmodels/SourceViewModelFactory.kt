package com.example.neuralnotesproject.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.neuralnotesproject.repository.SourceRepository

class SourceViewModelFactory(
    private val repository: SourceRepository,
    private val notebookId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SourceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SourceViewModel(repository, notebookId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
