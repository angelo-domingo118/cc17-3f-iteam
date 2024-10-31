package com.example.neuralnotesproject.data

data class Message(
    val content: String,
    val isUser: Boolean,
    val notebookId: String,
    val timestamp: Long = System.currentTimeMillis()
) 