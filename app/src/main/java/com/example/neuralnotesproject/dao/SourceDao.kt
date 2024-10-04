package com.example.neuralnotesproject.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.neuralnotesproject.data.Source

@Dao
interface SourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: Source)

    @Update
    suspend fun updateSource(source: Source)

    @Delete
    suspend fun deleteSource(source: Source)

    @Query("SELECT * FROM sources WHERE notebookId = :notebookId ORDER BY name ASC")
    fun getSourcesForNotebook(notebookId: String): LiveData<List<Source>>
}