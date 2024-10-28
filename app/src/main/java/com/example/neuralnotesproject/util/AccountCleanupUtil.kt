package com.example.neuralnotesproject.util

import android.content.Context
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.repository.NotebookRepository
import com.example.neuralnotesproject.repository.NoteRepository
import com.example.neuralnotesproject.repository.SourceRepository
import com.example.neuralnotesproject.repository.UserRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountCleanupUtil(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userRepository = UserRepository(database.userDao())
    private val notebookRepository = NotebookRepository(database.notebookDao())
    private val noteRepository = NoteRepository(database.noteDao())
    private val sourceRepository = SourceRepository(database.sourceDao())

    suspend fun cleanupUserData(userId: String) = withContext(Dispatchers.IO) {
        try {
            // 1. Delete all notebooks (this will cascade delete notes due to Room relationship)
            val notebooks = notebookRepository.getNotebooksForUserSync(userId)
            notebooks.forEach { notebook ->
                // Delete notebook files
                val notebookDir = File(context.filesDir, notebook.id)
                if (notebookDir.exists()) {
                    notebookDir.deleteRecursively()
                }
                // Delete from database
                notebookRepository.deleteNotebook(notebook)
            }

            // 2. Delete all sources
            val sources = sourceRepository.getSourcesForUser(userId)
            sources.forEach { source ->
                sourceRepository.deleteSource(source)
            }

            // 3. Clear user's preferences
            withContext(Dispatchers.Main) {
                val sharedPrefs = context.getSharedPreferences("user_${userId}_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().clear().apply()
            }

            // 4. Delete user from local database
            userRepository.getUserById(userId)?.let { user ->
                userRepository.deleteUser(user)
            }

            // 5. Clear any cached files in app's cache directory
            clearUserCache(userId)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun clearUserCache(userId: String) {
        // Clear user-specific cache directory
        val userCacheDir = File(context.cacheDir, userId)
        if (userCacheDir.exists()) {
            userCacheDir.deleteRecursively()
        }
    }
}
