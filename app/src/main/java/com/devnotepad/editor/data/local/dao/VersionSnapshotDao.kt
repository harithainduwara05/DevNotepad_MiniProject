package com.devnotepad.editor.data.local.dao

import androidx.room.*
import com.devnotepad.editor.data.local.entity.VersionSnapshotEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [VersionSnapshotEntity].
 *
 * Provides queries to manage version history for documents.
 * Version snapshots are ordered by version number for sequential
 * patch application during reconstruction.
 */
@Dao
interface VersionSnapshotDao {

    /** Insert a new version snapshot. Returns the auto-generated row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: VersionSnapshotEntity): Long

    /** Delete a specific snapshot. */
    @Delete
    suspend fun delete(snapshot: VersionSnapshotEntity)

    /**
     * Observe all snapshots for a document, ordered by version number.
     * Used to display version history in the UI.
     */
    @Query(
        """
        SELECT * FROM version_snapshots 
        WHERE document_id = :documentId 
        ORDER BY version_number ASC
        """
    )
    fun observeByDocument(documentId: Long): Flow<List<VersionSnapshotEntity>>

    /**
     * Get all snapshots up to (and including) a specific version number.
     * Used for reconstructing the document at a given version.
     * One-shot query (not reactive).
     */
    @Query(
        """
        SELECT * FROM version_snapshots 
        WHERE document_id = :documentId 
          AND version_number <= :upToVersion 
        ORDER BY version_number ASC
        """
    )
    suspend fun getSnapshotsUpTo(documentId: Long, upToVersion: Int): List<VersionSnapshotEntity>

    /**
     * Get the latest snapshot for a document.
     * Used to determine the current version number and last saved state.
     */
    @Query(
        """
        SELECT * FROM version_snapshots 
        WHERE document_id = :documentId 
        ORDER BY version_number DESC 
        LIMIT 1
        """
    )
    suspend fun getLatestSnapshot(documentId: Long): VersionSnapshotEntity?

    /**
     * Delete all snapshots for a document after a specific version.
     * Used when creating a new branch from a rolled-back version.
     */
    @Query(
        """
        DELETE FROM version_snapshots 
        WHERE document_id = :documentId 
          AND version_number > :afterVersion
        """
    )
    suspend fun deleteAfterVersion(documentId: Long, afterVersion: Int)

    /** Count total versions for a document. */
    @Query("SELECT COUNT(*) FROM version_snapshots WHERE document_id = :documentId")
    suspend fun countVersions(documentId: Long): Int
}
