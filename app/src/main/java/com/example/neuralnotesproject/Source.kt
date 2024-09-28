package com.example.neuralnotesproject

data class Source(
    val id: String,
    val name: String,
    val type: SourceType,
    val content: String
)

enum class SourceType {
    FILE, WEBSITE, TEXT
}