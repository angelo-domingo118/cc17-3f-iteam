package com.example.neuralnotesproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class NotebookViewModel : ViewModel() {
    private val _notebooks = MutableLiveData<List<Notebook>>()
    val notebooks: LiveData<List<Notebook>> = _notebooks

    fun addNotebook(notebook: Notebook) {
        val currentList = _notebooks.value.orEmpty().toMutableList()
        currentList.add(notebook)
        _notebooks.value = currentList
    }

    fun setNotebooks(notebooks: List<Notebook>) {
        _notebooks.value = notebooks
    }

    fun getNotebook(id: String): LiveData<Notebook?> {
        return notebooks.map { notebookList ->
            notebookList.find { it.id == id }
        }
    }

    fun deleteNotebook(position: Int): Notebook? {
        val currentList = _notebooks.value?.toMutableList() ?: return null
        return if (position in currentList.indices) {
            val deletedNotebook = currentList.removeAt(position)
            _notebooks.value = currentList
            deletedNotebook
        } else null
    }

    fun renameNotebook(position: Int, newTitle: String) {
        val currentList = _notebooks.value?.toMutableList() ?: return
        if (position in currentList.indices) {
            currentList[position] = currentList[position].copy(title = newTitle)
            _notebooks.value = currentList
        }
    }

    fun deleteNotebook(notebookId: String) {
        val currentList = _notebooks.value?.toMutableList() ?: mutableListOf()
        currentList.removeIf { it.id == notebookId }
        _notebooks.value = currentList
    }

    fun renameNotebook(notebookId: String, newTitle: String) {
        val currentList = _notebooks.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.id == notebookId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(title = newTitle)
            _notebooks.value = currentList
        }
    }

    // Remove the unused deleteNotebook(notebookId: String) function
}