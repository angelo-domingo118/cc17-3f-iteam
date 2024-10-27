package com.example.neuralnotesproject.viewmodels

import androidx.lifecycle.*
import com.example.neuralnotesproject.data.Notebook
import com.example.neuralnotesproject.repository.NotebookRepository
import kotlinx.coroutines.launch

class NotebookViewModel(
    private val repository: NotebookRepository,
    private val userId: String
) : ViewModel() {
    val notebooks: LiveData<List<Notebook>> = repository.getNotebooksForUser(userId)

    fun addNotebook(notebook: Notebook) = viewModelScope.launch {
        repository.insertNotebook(notebook)
    }

    fun deleteNotebook(notebook: Notebook) = viewModelScope.launch {
        repository.deleteNotebook(notebook)
    }

    fun updateNotebook(notebook: Notebook) = viewModelScope.launch {
        repository.updateNotebook(notebook)
    }

    fun getNotebook(id: String): LiveData<Notebook?> {
        return notebooks.map { notebookList ->
            notebookList.find { it.id == id }
        }
    }

    fun renameNotebook(notebookId: String, newTitle: String) = viewModelScope.launch {
        val notebook = notebooks.value?.find { it.id == notebookId }
        notebook?.let {
            val updatedNotebook = it.copy(title = newTitle)
            repository.updateNotebook(updatedNotebook)
        }
    }
}

class NotebookViewModelFactory(
    private val repository: NotebookRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotebookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotebookViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
