package com.example.neuralnotesproject.repository

import androidx.lifecycle.LiveData
import com.example.neuralnotesproject.data.Note
import com.example.neuralnotesproject.dao.NoteDao

class NoteRepository(private val noteDao: NoteDao) {
    fun getNotesForNotebook(notebookId: String): LiveData<List<Note>> = 
        noteDao.getNotesForNotebook(notebookId)

    suspend fun insertNote(note: Note) = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
}
