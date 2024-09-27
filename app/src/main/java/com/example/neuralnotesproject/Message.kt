package com.example.neuralnotesproject

data class Message(
    val content: String,
    val isUser: Boolean,
    var isLiked: Boolean = false,
    var isDisliked: Boolean = false
)