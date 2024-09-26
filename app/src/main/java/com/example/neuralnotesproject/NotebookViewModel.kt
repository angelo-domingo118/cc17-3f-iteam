package com.example.neuralnotesproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class NotebookViewModel : ViewModel() {
    private val _notebooks = MutableLiveData<List<Notebook>>(emptyList())
    val notebooks: LiveData<List<Notebook>> = _notebooks

    fun addNotebook(notebook: Notebook) {
        val currentList = _notebooks.value ?: emptyList()
        _notebooks.value = listOf(notebook) + currentList
    }

    fun getNotebook(id: String): LiveData<Notebook?> {
        return notebooks.map { notebookList ->
            notebookList.find { it.id == id }
        }
    }

    fun deleteNotebook(position: Int) {
        val currentList = _notebooks.value?.toMutableList() ?: return
        if (position in currentList.indices) {
            currentList.removeAt(position)
            _notebooks.value = currentList
        }
    }

    fun renameNotebook(position: Int, newTitle: String) {
        val currentList = _notebooks.value?.toMutableList() ?: return
        if (position in currentList.indices) {
            currentList[position] = currentList[position].copy(title = newTitle)
            _notebooks.value = currentList
        }
    }

    fun deleteNotebook(notebookId: String) {
        // Implement the logic to delete the notebook from your data source
        // This might involve calling a repository method or directly accessing a database
        // After deletion, you might want to update any live data that's observing the notebooks
    }
}