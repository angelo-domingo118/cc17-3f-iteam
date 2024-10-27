package com.example.neuralnotesproject.util

import android.content.Context
import android.util.Log
import com.example.neuralnotesproject.data.Notebook
import com.example.neuralnotesproject.data.Note
import com.example.neuralnotesproject.data.User
import com.example.neuralnotesproject.repository.NotebookRepository
import com.example.neuralnotesproject.repository.NoteRepository
import com.example.neuralnotesproject.repository.UserRepository
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object DataMigrationUtil {
    suspend fun migrateData(
        context: Context,
        userId: String,
        userEmail: String,
        username: String,
        notebookRepository: NotebookRepository,
        noteRepository: NoteRepository,
        userRepository: UserRepository
    ) {
        // First, ensure user exists in database
        val user = User(
            id = userId,
            email = userEmail,
            username = username
        )
        userRepository.insertUser(user)

        // Then migrate notebooks
        context.filesDir.listFiles()?.forEach { folder ->
            if (folder.isDirectory) {
                val file = File(folder, "notebook_details.txt")
                if (file.exists()) {
                    try {
                        val lines = file.readLines()
                        if (lines.size >= 3) {
                            val notebook = Notebook(
                                id = lines[0],
                                userId = userId,
                                title = lines[1],
                                creationDate = lines[2]
                            )
                            notebookRepository.insertNotebook(notebook)
                            
                            // Migrate notes for this notebook
                            migrateNotes(context, notebook.id, noteRepository)
                        }
                    } catch (e: IOException) {
                        Log.e("DataMigration", "Error migrating notebook", e)
                    }
                }
            }
        }
    }

    private suspend fun migrateNotes(
        context: Context,
        notebookId: String,
        noteRepository: NoteRepository
    ) {
        val notesFolder = File(context.filesDir, "$notebookId/notes")
        if (notesFolder.exists() && notesFolder.isDirectory) {
            notesFolder.listFiles()?.forEach { file ->
                try {
                    val lines = file.readLines()
                    if (lines.isNotEmpty()) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val note = Note(
                            id = file.nameWithoutExtension,
                            notebookId = notebookId,
                            title = lines[0],
                            content = lines.drop(1).joinToString("\n"),
                            creationDate = dateFormat.format(Date(file.lastModified()))
                        )
                        noteRepository.insertNote(note)
                    }
                } catch (e: IOException) {
                    Log.e("DataMigration", "Error migrating note", e)
                }
            }
        }
    }
}
