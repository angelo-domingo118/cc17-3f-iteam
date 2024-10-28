package com.example.neuralnotesproject.repository

import androidx.lifecycle.LiveData
import com.example.neuralnotesproject.dao.SourceDao
import com.example.neuralnotesproject.data.Source

class SourceRepository(private val sourceDao: SourceDao) {

    fun getSourcesForNotebook(notebookId: String): LiveData<List<Source>> {
        return sourceDao.getSourcesForNotebook(notebookId)
    }

    suspend fun getSourcesForUser(userId: String): List<Source> {
        return sourceDao.getSourcesForUser(userId)
    }

    suspend fun insertSource(source: Source) {
        sourceDao.insertSource(source)
    }

    suspend fun updateSource(source: Source) {
        sourceDao.updateSource(source)
    }

    suspend fun deleteSource(source: Source) {
        sourceDao.deleteSource(source)
    }
}
