package com.example.neuralnotesproject.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.neuralnotesproject.data.Notebook

@Dao
interface NotebookDao {

    @Query("SELECT * FROM notebooks WHERE userId = :userId ORDER BY creationDate DESC")
    fun getNotebooksForUser(userId: String): LiveData<List<Notebook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: Notebook)

    @Update
    suspend fun updateNotebook(notebook: Notebook)

    @Delete
    suspend fun deleteNotebook(notebook: Notebook)

    @Query("SELECT * FROM notebooks WHERE userId = :userId")
    fun getNotebooksForUserSync(userId: String): List<Notebook>
}
