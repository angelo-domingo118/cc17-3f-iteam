package com.example.neuralnotesproject

import android.content.Context
import java.io.File
import java.io.IOException

object FileUtils {
    private const val NOTES_FOLDER = "notes"

    fun saveNote(context: Context, notebookId: String, note: Note) {
        val notebookFolder = File(context.filesDir, notebookId)
        val notesFolder = File(notebookFolder, NOTES_FOLDER)
        notesFolder.mkdirs()

        val noteFile = File(notesFolder, "${note.id}.txt")
        noteFile.writeText("${note.title}\n${note.content}")
    }

    fun loadNotes(context: Context, notebookId: String): List<Note> {
        val notebookFolder = File(context.filesDir, notebookId)
        val notesFolder = File(notebookFolder, NOTES_FOLDER)
        val notes = mutableListOf<Note>()

        if (notesFolder.exists()) {
            notesFolder.listFiles()?.forEach { file ->
                try {
                    val lines = file.readLines()
                    if (lines.isNotEmpty()) {
                        val id = file.nameWithoutExtension
                        val title = lines[0]
                        val content = if (lines.size > 1) lines.subList(1, lines.size).joinToString("\n") else ""
                        notes.add(Note(id, title, content))
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        return notes
    }

    fun deleteNote(context: Context, notebookId: String, noteId: String) {
        val notebookFolder = File(context.filesDir, notebookId)
        val notesFolder = File(notebookFolder, NOTES_FOLDER)
        val noteFile = File(notesFolder, "$noteId.txt")
        noteFile.delete()
    }
}