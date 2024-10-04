package com.example.neuralnotesproject.repository

import androidx.lifecycle.LiveData
import com.example.neuralnotesproject.dao.NotebookDao
import com.example.neuralnotesproject.data.Notebook

class NotebookRepository(private val notebookDao: NotebookDao) {

    fun getNotebooksForUser(userId: Int): LiveData<List<Notebook>> {
        return notebookDao.getNotebooksForUser(userId)
    }

    suspend fun insertNotebook(notebook: Notebook) {
        notebookDao.insertNotebook(notebook)
    }

    suspend fun updateNotebook(notebook: Notebook) {
        notebookDao.updateNotebook(notebook)
    }

    suspend fun deleteNotebook(notebook: Notebook) {
        notebookDao.deleteNotebook(notebook)
    }
}