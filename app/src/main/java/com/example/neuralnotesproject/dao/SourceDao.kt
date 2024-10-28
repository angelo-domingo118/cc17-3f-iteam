package com.example.neuralnotesproject.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.neuralnotesproject.data.Source

@Dao
interface SourceDao {
    @Query("""
        SELECT s.* FROM sources s
        INNER JOIN notebooks n ON s.notebookId = n.id
        WHERE n.userId = :userId
    """)
    suspend fun getSourcesForUser(userId: String): List<Source>

    @Query("SELECT * FROM sources WHERE notebookId = :notebookId")
    fun getSourcesForNotebook(notebookId: String): LiveData<List<Source>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: Source)

    @Update
    suspend fun updateSource(source: Source)

    @Delete
    suspend fun deleteSource(source: Source)
}
