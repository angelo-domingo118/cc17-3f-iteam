package com.example.neuralnotesproject.data

import java.util.*

data class Message(
    val content: String,
    val isUser: Boolean,
    val notebookId: String,
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis()
) 