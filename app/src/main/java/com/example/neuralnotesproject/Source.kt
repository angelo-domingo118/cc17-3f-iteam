package com.example.neuralnotesproject

data class Source(
    val id: String,
    val name: String,
    val type: SourceType,
    val content: String // For FILE type, this should be the file content, not the URI
)
enum class SourceType {
    FILE, WEBSITE, TEXT
}
