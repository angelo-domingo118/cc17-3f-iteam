package com.example.neuralnotesproject.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.neuralnotesproject.dao.*
import com.example.neuralnotesproject.data.Chat
import com.example.neuralnotesproject.data.Note
import com.example.neuralnotesproject.data.Notebook
import com.example.neuralnotesproject.data.Source
import com.example.neuralnotesproject.data.User

@Database(
    entities = [
        User::class,
        Notebook::class,
        Note::class,
        Source::class,
        Chat::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun notebookDao(): NotebookDao
    abstract fun noteDao(): NoteDao
    abstract fun sourceDao(): SourceDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Define migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add filePath column to sources table
                db.execSQL(
                    "ALTER TABLE sources ADD COLUMN filePath TEXT"
                )
            }
        }
    }
}
