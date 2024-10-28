package com.example.neuralnotesproject.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create the sources table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS sources (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                content TEXT NOT NULL,
                notebookId TEXT NOT NULL,
                FOREIGN KEY (notebookId) REFERENCES notebooks(id) ON DELETE CASCADE
            )
        """)
        
        // Create the chats table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS chats (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                notebookId INTEGER NOT NULL,
                userId TEXT NOT NULL,
                message TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                isUserMessage INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (notebookId) REFERENCES notebooks(id) ON DELETE CASCADE
            )
        """)
    }
}
