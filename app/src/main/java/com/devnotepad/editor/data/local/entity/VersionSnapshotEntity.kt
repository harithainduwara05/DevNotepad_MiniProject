package com.devnotepad.editor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single version snapshot (patch) for a document.
 *
 * Version control strategy:
 *  - Version 1: [patchData] contains the FULL text of the document.
 *  - Version 2+: [patchData] contains a unified diff (patch) calculated
 *    by java-diff-utils, representing the delta from the previous version.
 *
 * To reconstruct a document at version N:
 *  1. Load version 1 (full text).
 *  2. Sequentially apply patches 2..N.
 *
 * @property id Auto-generated primary key.
 * @property documentId Foreign key referencing [DocumentEntity.id].
 * @property versionNumber Sequential version number starting at 1.
 * @property timestamp Epoch millis when this version was saved.
 * @property patchData The full text (v1) or unified diff string (v2+).
 * @property description Optional user-provided commit message.
 */
@Entity(
    tableName = "version_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE // Delete snapshots when document is removed
        )
    ],
    indices = [
        Index(value = ["document_id", "version_number"], unique = true)
    ]
)
data class VersionSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "document_id")
    val documentId: Long,

    @ColumnInfo(name = "version_number")
    val versionNumber: Int,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "patch_data")
    val patchData: String,

    @ColumnInfo(name = "description")
    val description: String? = null
)
