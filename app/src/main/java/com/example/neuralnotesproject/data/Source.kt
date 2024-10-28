package com.example.neuralnotesproject.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "sources",
    foreignKeys = [
        ForeignKey(
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["notebookId"])]
)
data class Source(
    @PrimaryKey val id: String,
    val name: String,
    val type: SourceType,
    val content: String,
    val notebookId: String
)

enum class SourceType {
    PASTE_TEXT,
    WEBSITE,
    FILE
}
