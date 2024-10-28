package com.example.neuralnotesproject.repository

import androidx.lifecycle.LiveData
import com.example.neuralnotesproject.dao.NotebookDao
import com.example.neuralnotesproject.data.Notebook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotebookRepository(private val notebookDao: NotebookDao) {

    fun getNotebooksForUser(userId: String): LiveData<List<Notebook>> {
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

    suspend fun getNotebooksForUserSync(userId: String): List<Notebook> = withContext(Dispatchers.IO) {
        notebookDao.getNotebooksForUserSync(userId)
    }
}
