package com.example.neuralnotesproject.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class SourceType {
    PASTE_TEXT,
    WEBSITE,
    FILE
}

@Entity(
    tableName = "sources",
    foreignKeys = [
        ForeignKey(
            entity = Notebook::class, // Reference to Notebook entity
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Source(
    @PrimaryKey val id: String,
    val name: String,
    val type: SourceType,
    val content: String, // For PASTE_TEXT: text content, WEBSITE: URL, FILE: file path
    @ColumnInfo(index = true) val notebookId: String
)