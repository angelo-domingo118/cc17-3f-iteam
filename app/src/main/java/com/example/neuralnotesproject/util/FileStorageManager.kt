package com.example.neuralnotesproject.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

class FileStorageManager(private val context: Context) {
    
    fun saveFile(uri: Uri, notebookId: String): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "${UUID.randomUUID()}_${uri.lastPathSegment}"
        val directory = File(context.filesDir, "notebooks/$notebookId/files")
        directory.mkdirs()
        
        val file = File(directory, fileName)
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        return file.absolutePath
    }

    fun getFile(filePath: String): File {
        return File(filePath)
    }

    fun deleteFile(filePath: String) {
        File(filePath).delete()
    }
}
