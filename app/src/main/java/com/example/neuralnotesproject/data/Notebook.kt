package com.example.neuralnotesproject.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val userId: String,
    var title: String,
    val creationDate: String
)
