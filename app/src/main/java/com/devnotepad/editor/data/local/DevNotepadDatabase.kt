package com.devnotepad.editor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.devnotepad.editor.data.local.dao.DocumentDao
import com.devnotepad.editor.data.local.dao.VersionSnapshotDao
import com.devnotepad.editor.data.local.entity.DocumentEntity
import com.devnotepad.editor.data.local.entity.VersionSnapshotEntity

/**
 * Room Database for DevNotepad.
 *
 * Manages two tables:
 *  - [DocumentEntity]: Tracks known documents (id, name, path).
 *  - [VersionSnapshotEntity]: Stores version patches (deltas) for each document.
 *
 * Uses the singleton pattern with double-checked locking to ensure only
 * one database connection exists across the entire app lifecycle.
 */
@Database(
    entities = [
        DocumentEntity::class,
        VersionSnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DevNotepadDatabase : RoomDatabase() {

    /** DAO for document CRUD operations */
    abstract fun documentDao(): DocumentDao

    /** DAO for version snapshot CRUD operations */
    abstract fun versionSnapshotDao(): VersionSnapshotDao

    companion object {
        private const val DATABASE_NAME = "devnotepad.db"

        @Volatile
        private var INSTANCE: DevNotepadDatabase? = null

        /**
         * Returns the singleton database instance, creating it if needed.
         * Thread-safe via synchronized block with volatile backing field.
         */
        fun getInstance(context: Context): DevNotepadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DevNotepadDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // OK for v1; add migrations later
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
