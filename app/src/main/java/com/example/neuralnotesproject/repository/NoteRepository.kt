package com.example.neuralnotesproject.repository

import androidx.lifecycle.LiveData
import com.example.neuralnotesproject.dao.NoteDao
import com.example.neuralnotesproject.data.Note

class NoteRepository(private val noteDao: NoteDao) {

    fun getNotesForNotebook(notebookId: String): LiveData<List<Note>> {
        return noteDao.getNotesForNotebook(notebookId)
    }

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }
}