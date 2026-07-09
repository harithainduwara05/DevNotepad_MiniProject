package com.devnotepad.editor.data.local.dao

import androidx.room.*
import com.devnotepad.editor.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [DocumentEntity].
 *
 * All queries return Flows for reactive UI updates, except one-shot
 * operations like insert/update/delete which are suspend functions
 * executed within coroutine scopes.
 */
@Dao
interface DocumentDao {

    /** Insert a new document. Returns the auto-generated row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity): Long

    /** Update an existing document's metadata. */
    @Update
    suspend fun update(document: DocumentEntity)

    /** Delete a document (cascades to its version snapshots). */
    @Delete
    suspend fun delete(document: DocumentEntity)

    /** Observe all documents ordered by last modified (most recent first). */
    @Query("SELECT * FROM documents ORDER BY last_modified_at DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    /** Find a document by its file path (one-shot). */
    @Query("SELECT * FROM documents WHERE file_path = :path LIMIT 1")
    suspend fun findByPath(path: String): DocumentEntity?

    /** Find a document by its ID (one-shot). */
    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): DocumentEntity?
}
