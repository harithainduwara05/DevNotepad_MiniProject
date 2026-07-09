package com.devnotepad.editor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a tracked document in the version control system.
 *
 * Each document has a unique path on the filesystem. The Room table
 * stores metadata used to associate version snapshots with their
 * parent document.
 *
 * @property id Auto-generated primary key.
 * @property name Display name of the file (e.g., "Main.kt").
 * @property filePath Absolute path to the file on local storage.
 * @property createdAt Epoch millis when the document was first tracked.
 * @property lastModifiedAt Epoch millis of the most recent save/version.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "file_path", index = true)
    val filePath: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_modified_at")
    val lastModifiedAt: Long = System.currentTimeMillis()
)
