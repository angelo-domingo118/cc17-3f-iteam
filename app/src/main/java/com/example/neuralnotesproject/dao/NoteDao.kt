package com.example.neuralnotesproject.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.neuralnotesproject.data.Note

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE notebookId = :notebookId ORDER BY creationDate DESC")
    fun getNotesForNotebook(notebookId: String): LiveData<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}
