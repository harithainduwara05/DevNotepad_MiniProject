package com.devnotepad.editor.data.repository

import com.devnotepad.editor.data.local.AutoSaveData
import com.devnotepad.editor.data.local.DiffEngine
import com.devnotepad.editor.data.local.DiffLine
import com.devnotepad.editor.data.local.FileManager
import com.devnotepad.editor.data.local.dao.DocumentDao
import com.devnotepad.editor.data.local.dao.VersionSnapshotDao
import com.devnotepad.editor.data.local.entity.DocumentEntity
import com.devnotepad.editor.data.local.entity.VersionSnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Central repository orchestrating all storage operations for DevNotepad.
 *
 * This is the single point of contact between the ViewModel layer and the
 * data layer. It coordinates:
 *
 *  1. **File I/O** — Reading/writing actual text files via [FileManager].
 *  2. **Version Control** — Computing diffs, storing patches, and
 *     reconstructing past versions via [DiffEngine] and Room DAOs.
 *  3. **Auto-Save** — Managing crash-recovery temp files.
 *  4. **Document Metadata** — Tracking documents in the Room database.
 *
 * Design principle: The repository decides WHERE to store data (Room vs.
 * filesystem) while the ViewModel decides WHEN operations happen.
 *
 * @param documentDao Room DAO for document metadata.
 * @param versionSnapshotDao Room DAO for version snapshots.
 * @param fileManager Handles raw file I/O.
 * @param diffEngine Computes and applies diffs.
 */
class DocumentRepository(
    private val documentDao: DocumentDao,
    private val versionSnapshotDao: VersionSnapshotDao,
    private val fileManager: FileManager,
    private val diffEngine: DiffEngine
) {

    // ─────────────────────────────────────────────────────────────────
    // Document Metadata Operations
    // ─────────────────────────────────────────────────────────────────

    /**
     * Observes all tracked documents, ordered by most recently modified.
     * Used to populate the "Recent Files" sidebar.
     */
    fun observeAllDocuments(): Flow<List<DocumentEntity>> {
        return documentDao.observeAll()
    }

    /**
     * Finds or creates a document entry for the given file path.
     *
     * If the document already exists in Room, returns it.
     * Otherwise, creates a new entry and returns it with the generated ID.
     *
     * @param path Absolute file path.
     * @param name Display name of the file.
     * @return The [DocumentEntity] with a valid ID.
     */
    suspend fun getOrCreateDocument(path: String, name: String): DocumentEntity {
        // Check if document already exists in the database
        val existing = documentDao.findByPath(path)
        if (existing != null) {
            return existing
        }

        // Create new document entry
        val newDoc = DocumentEntity(
            name = name,
            filePath = path
        )
        val id = documentDao.insert(newDoc)
        return newDoc.copy(id = id)
    }

    /**
     * Updates the lastModifiedAt timestamp for a document.
     */
    suspend fun touchDocument(document: DocumentEntity) {
        documentDao.update(document.copy(lastModifiedAt = System.currentTimeMillis()))
    }

    /**
     * Finds a document by its database ID.
     */
    suspend fun findDocumentById(id: Long): DocumentEntity? {
        return documentDao.findById(id)
    }

    /**
     * Deletes a document and all its version history.
     * The CASCADE foreign key ensures snapshots are also deleted.
     */
    suspend fun deleteDocument(document: DocumentEntity) {
        documentDao.delete(document)
    }

    // ─────────────────────────────────────────────────────────────────
    // File I/O Operations
    // ─────────────────────────────────────────────────────────────────

    /**
     * Opens a file and reads its text content.
     *
     * @param path Absolute file path.
     * @return The file content as a string.
     */
    suspend fun openFile(path: String): String {
        return fileManager.readFile(path)
    }

    /**
     * Saves text content to a file on the filesystem.
     *
     * This writes the actual file (e.g., to Downloads or Documents).
     * Version tracking is handled separately by [saveVersion].
     *
     * @param path Absolute file path.
     * @param content Text content to write.
     */
    suspend fun saveFile(path: String, content: String) {
        fileManager.writeFile(path, content)
    }

    /**
     * Checks if a file exists at the given path.
     */
    suspend fun fileExists(path: String): Boolean {
        return fileManager.fileExists(path)
    }

    /**
     * Extracts the filename from a path.
     */
    fun extractFileName(path: String): String {
        return fileManager.extractFileName(path)
    }

    // ─────────────────────────────────────────────────────────────────
    // Version Control Operations (Delta-Based)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Saves a new version of a document using incremental delta storage.
     *
     * Strategy:
     *  - **First save (v1):** Stores the full text as the base version.
     *  - **Subsequent saves (v2+):** Computes a unified diff against the
     *    previous version and stores only the patch.
     *
     * @param documentId The Room ID of the document.
     * @param currentText The current text content to save as a new version.
     * @param description Optional commit message for this version.
     * @return The created [VersionSnapshotEntity].
     */
    suspend fun saveVersion(
        documentId: Long,
        currentText: String,
        description: String? = null
    ): VersionSnapshotEntity = withContext(Dispatchers.IO) {
        val latestSnapshot = versionSnapshotDao.getLatestSnapshot(documentId)
        val newVersionNumber = (latestSnapshot?.versionNumber ?: 0) + 1

        val patchData: String

        if (newVersionNumber == 1) {
            // ── FIRST VERSION: Store full text ──
            // Save the complete text as the base for future diffs
            fileManager.saveBaseVersion(documentId, currentText)
            patchData = currentText // Also store in Room for quick access
        } else {
            // ── SUBSEQUENT VERSION: Store delta only ──
            // Reconstruct the previous version to compute the diff against
            val previousText = reconstructVersion(documentId, latestSnapshot!!.versionNumber)

            // Compute unified diff between previous and current
            val document = documentDao.findById(documentId)
            val fileName = document?.name ?: "file"
            patchData = diffEngine.generatePatch(previousText, currentText, fileName)

            // If the diff is empty, texts are identical — skip saving
            if (patchData.isEmpty()) {
                return@withContext latestSnapshot
            }

            // Store the patch file for filesystem-level backup
            fileManager.savePatch(documentId, newVersionNumber, patchData)
        }

        // Insert the snapshot into Room
        val snapshot = VersionSnapshotEntity(
            documentId = documentId,
            versionNumber = newVersionNumber,
            patchData = patchData,
            description = description
        )
        val id = versionSnapshotDao.insert(snapshot)

        // Update document's lastModifiedAt
        documentDao.findById(documentId)?.let { doc ->
            documentDao.update(doc.copy(lastModifiedAt = System.currentTimeMillis()))
        }

        snapshot.copy(id = id)
    }

    /**
     * Reconstructs the full text of a document at a specific version.
     *
     * Algorithm:
     *  1. Load the base text (version 1).
     *  2. Fetch all snapshots from version 2 up to the target version.
     *  3. Sequentially apply each patch to build up to the target version.
     *
     * @param documentId The Room ID of the document.
     * @param targetVersion The version number to reconstruct.
     * @return The full text content at the specified version.
     * @throws IllegalStateException if base version is missing.
     */
    suspend fun reconstructVersion(
        documentId: Long,
        targetVersion: Int
    ): String = withContext(Dispatchers.Default) {
        // Get all snapshots up to the target version
        val snapshots = versionSnapshotDao.getSnapshotsUpTo(documentId, targetVersion)

        if (snapshots.isEmpty()) {
            throw IllegalStateException("No snapshots found for document $documentId")
        }

        // Version 1 contains the full base text
        var currentText = snapshots.first().patchData

        // Apply each subsequent patch sequentially
        for (i in 1 until snapshots.size) {
            val patch = snapshots[i].patchData
            if (patch.isNotBlank()) {
                currentText = diffEngine.applyPatch(currentText, patch)
            }
        }

        currentText
    }

    /**
     * Rolls back a document to a specific version.
     *
     * This reconstructs the text at the target version, deletes all
     * snapshots after it, and writes the reconstructed text to the file.
     *
     * @param documentId The Room ID of the document.
     * @param targetVersion The version to roll back to.
     * @return The reconstructed text at the target version.
     */
    suspend fun rollbackToVersion(
        documentId: Long,
        targetVersion: Int
    ): String = withContext(Dispatchers.IO) {
        // Reconstruct the text at the target version
        val restoredText = reconstructVersion(documentId, targetVersion)

        // Delete all snapshots after the target version (in Room)
        versionSnapshotDao.deleteAfterVersion(documentId, targetVersion)

        // Delete corresponding patch files from filesystem
        fileManager.deletePatchesAfter(documentId, targetVersion)

        // Write the restored text to the actual file
        val document = documentDao.findById(documentId)
        if (document != null) {
            fileManager.writeFile(document.filePath, restoredText)
            documentDao.update(document.copy(lastModifiedAt = System.currentTimeMillis()))
        }

        restoredText
    }

    /**
     * Computes a line-by-line diff between two versions of a document.
     * Used by the Diff View UI in Phase 5.
     *
     * @param documentId The Room ID of the document.
     * @param fromVersion The older version number.
     * @param toVersion The newer version number.
     * @return A list of [DiffLine] entries for UI rendering.
     */
    suspend fun computeDiffBetweenVersions(
        documentId: Long,
        fromVersion: Int,
        toVersion: Int
    ): List<DiffLine> {
        val fromText = reconstructVersion(documentId, fromVersion)
        val toText = reconstructVersion(documentId, toVersion)
        return diffEngine.computeLineDiff(fromText, toText)
    }

    /**
     * Gets the version history for a document (reactive Flow).
     */
    fun observeVersionHistory(documentId: Long): Flow<List<VersionSnapshotEntity>> {
        return versionSnapshotDao.observeByDocument(documentId)
    }

    /**
     * Gets the latest version number for a document.
     */
    suspend fun getLatestVersionNumber(documentId: Long): Int {
        return versionSnapshotDao.getLatestSnapshot(documentId)?.versionNumber ?: 0
    }

    /**
     * Gets the total number of versions for a document.
     */
    suspend fun getVersionCount(documentId: Long): Int {
        return versionSnapshotDao.countVersions(documentId)
    }

    // ─────────────────────────────────────────────────────────────────
    // Auto-Save / Crash Recovery
    // ─────────────────────────────────────────────────────────────────

    /**
     * Writes the current editor buffer to the auto-save cache.
     * Called every 10 seconds by the ViewModel's auto-save coroutine.
     *
     * @param originalPath The file path currently being edited.
     * @param content The current text in the editor buffer.
     */
    suspend fun writeAutoSave(originalPath: String, content: String) {
        fileManager.writeAutoSave(originalPath, content)
    }

    /**
     * Checks for a recoverable auto-save from a previous crash.
     *
     * @return [AutoSaveData] if recovery is available, null otherwise.
     */
    suspend fun checkAutoSave(): AutoSaveData? {
        return fileManager.checkAutoSave()
    }

    /**
     * Clears all auto-save cache files.
     * Called after a successful manual save or when recovery is dismissed.
     */
    suspend fun clearAutoSave() {
        fileManager.clearAutoSave()
    }

    /**
     * Clears auto-save for a specific file.
     */
    suspend fun clearAutoSaveForPath(path: String) {
        fileManager.clearAutoSaveForPath(path)
    }
}
