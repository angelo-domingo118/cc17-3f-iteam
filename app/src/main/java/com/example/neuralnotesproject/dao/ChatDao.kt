package com.example.neuralnotesproject.dao

import androidx.room.*
import com.example.neuralnotesproject.data.Chat
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE notebookId = :notebookId ORDER BY timestamp ASC")
    fun getChatsByNotebook(notebookId: Long): Flow<List<Chat>>

    @Insert
    suspend fun insert(chat: Chat)

    @Delete
    suspend fun delete(chat: Chat)

    @Query("DELETE FROM chats WHERE notebookId = :notebookId")
    suspend fun deleteAllChatsFromNotebook(notebookId: Long)
}
