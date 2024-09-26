package com.example.neuralnotesproject

data class Source(
    val id: String,
    val name: String,
    val type: SourceType
)

enum class SourceType {
    FILE, WEBSITE, TEXT
}