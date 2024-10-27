package com.example.neuralnotesproject.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,  // Firebase user ID
    val email: String,
    val username: String
)
