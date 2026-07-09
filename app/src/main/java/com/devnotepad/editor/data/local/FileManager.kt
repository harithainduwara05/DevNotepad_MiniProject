package com.devnotepad.editor.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Handles all raw file I/O operations for the editor.
 *
 * Responsibilities:
 *  - Reading/writing text files with UTF-8 encoding.
 *  - Managing the app-private storage directories for version data.
 *  - Providing safe atomic writes (write to temp → rename) to prevent
 *    data loss if the app is killed mid-write.
 *
 * All operations are suspend functions that run on [Dispatchers.IO].
 */
class FileManager(private val context: Context) {

    // ─────────────────────────────────────────────────────────────────
    // Directory structure within app-private storage:
    //   files/
    //   ├── versions/       ← Full base texts for version 1 of each doc
    //   ├── patches/        ← Delta patch files for subsequent versions
    //   └── autosave/       ← Crash-recovery temp files
    // ─────────────────────────────────────────────────────────────────

    /** Directory for storing full base text of version 1 */
    private val versionsDir: File
        get() = File(context.filesDir, "versions").also { it.mkdirs() }

    /** Directory for storing delta patch files */
    private val patchesDir: File
        get() = File(context.filesDir, "patches").also { it.mkdirs() }

    /** Directory for auto-save crash recovery files */
    val autoSaveDir: File
        get() = File(context.filesDir, "autosave").also { it.mkdirs() }

    // ─────────────────────────────────────────────────────────────────
    // General File I/O
    // ─────────────────────────────────────────────────────────────────

    /**
     * Reads a text file from the given absolute path.
     *
     * @param path Absolute file path to read from.
     * @return The file contents as a UTF-8 string.
     * @throws IOException if the file doesn't exist or can't be read.
     */
    suspend fun readFile(path: String): String = withContext(Dispatchers.IO) {
        if (path.startsWith("content://")) {
            val uri = android.net.Uri.parse(path)
            return@withContext context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
            } ?: throw IOException("Could not open input stream for URI: $path")
        }
        val file = File(path)
        if (!file.exists()) {
            throw IOException("File not found: $path")
        }
        file.readText(StandardCharsets.UTF_8)
    }

    /**
     * Writes text content to the given absolute path using atomic write.
     *
     * Strategy: Write to a temporary ".tmp" sibling file first, then
     * rename it to the target. This prevents corruption if the process
     * is killed during the write.
     *
     * @param path Absolute file path to write to.
     * @param content The text content to write.
     * @throws IOException if the write or rename fails.
     */
    suspend fun writeFile(path: String, content: String) = withContext(Dispatchers.IO) {
        if (path.startsWith("content://")) {
            val uri = android.net.Uri.parse(path)
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
                } ?: throw IOException("Could not open output stream for URI: $path")
            } catch (e: Exception) {
                throw IOException("Failed to write file: $path", e)
            }
            return@withContext
        }

        val targetFile = File(path)
        val parentDir = targetFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        // Atomic write: temp file → rename to target
        val tempFile = File(parentDir, "${targetFile.name}.tmp")
        try {
            tempFile.writeText(content, StandardCharsets.UTF_8)
            // Delete existing target before rename (required on some filesystems)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                // Fallback: if rename fails (cross-mount), copy and delete
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            // Clean up temp file on failure
            tempFile.delete()
            throw IOException("Failed to write file: $path", e)
        }
    }

    /**
     * Checks if a file exists at the given path.
     */
    suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        if (path.startsWith("content://")) {
            val uri = android.net.Uri.parse(path)
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    return@withContext cursor.moveToFirst()
                }
            } catch (e: Exception) {
                // Ignore and fallback
            }
            return@withContext false
        }
        File(path).exists()
    }

    /**
     * Extracts the file name from an absolute path.
     * Example: "/storage/emulated/0/Documents/Main.kt" → "Main.kt"
     */
    fun extractFileName(path: String): String {
        if (path.startsWith("content://")) {
            val uri = android.net.Uri.parse(path)
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                // Ignore and fallback
            }
            return "untitled"
        }
        return File(path).name
    }

    // ─────────────────────────────────────────────────────────────────
    // Version Storage (app-private directories)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Saves the full base text for version 1 of a document.
     *
     * File is stored as: versions/{documentId}_base.txt
     *
     * @param documentId The Room database ID of the document.
     * @param content Full text content of the first version.
     */
    suspend fun saveBaseVersion(documentId: Long, content: String) =
        withContext(Dispatchers.IO) {
            val file = File(versionsDir, "${documentId}_base.txt")
            file.writeText(content, StandardCharsets.UTF_8)
        }

    /**
     * Reads the full base text (version 1) of a document.
     *
     * @param documentId The Room database ID of the document.
     * @return The full text of version 1, or null if not found.
     */
    suspend fun readBaseVersion(documentId: Long): String? =
        withContext(Dispatchers.IO) {
            val file = File(versionsDir, "${documentId}_base.txt")
            if (file.exists()) file.readText(StandardCharsets.UTF_8) else null
        }

    /**
     * Saves a delta patch for a specific version of a document.
     *
     * File is stored as: patches/{documentId}_v{versionNumber}.patch
     *
     * @param documentId The Room database ID of the document.
     * @param versionNumber The version number this patch creates.
     * @param patchData The unified diff string.
     */
    suspend fun savePatch(documentId: Long, versionNumber: Int, patchData: String) =
        withContext(Dispatchers.IO) {
            val file = File(patchesDir, "${documentId}_v${versionNumber}.patch")
            file.writeText(patchData, StandardCharsets.UTF_8)
        }

    /**
     * Reads a delta patch for a specific version.
     *
     * @param documentId The Room database ID of the document.
     * @param versionNumber The version number to read the patch for.
     * @return The unified diff string, or null if not found.
     */
    suspend fun readPatch(documentId: Long, versionNumber: Int): String? =
        withContext(Dispatchers.IO) {
            val file = File(patchesDir, "${documentId}_v${versionNumber}.patch")
            if (file.exists()) file.readText(StandardCharsets.UTF_8) else null
        }

    /**
     * Deletes all patch files for a document after a specific version.
     * Used during rollback to discard future versions.
     *
     * @param documentId The Room database ID of the document.
     * @param afterVersion Delete patches with version numbers > this value.
     */
    suspend fun deletePatchesAfter(documentId: Long, afterVersion: Int) =
        withContext(Dispatchers.IO) {
            patchesDir.listFiles()?.forEach { file ->
                val prefix = "${documentId}_v"
                if (file.name.startsWith(prefix) && file.name.endsWith(".patch")) {
                    val versionStr = file.name
                        .removePrefix(prefix)
                        .removeSuffix(".patch")
                    val version = versionStr.toIntOrNull()
                    if (version != null && version > afterVersion) {
                        file.delete()
                    }
                }
            }
        }

    // ─────────────────────────────────────────────────────────────────
    // Auto-Save / Crash Recovery
    // ─────────────────────────────────────────────────────────────────

    /**
     * Saves the current editor buffer to a temporary auto-save file.
     *
     * File is stored as: autosave/autosave_{sanitizedPath}.tmp
     * A companion metadata file stores the original file path.
     *
     * @param originalPath The original file path being edited (or "untitled").
     * @param content The current text content of the editor buffer.
     */
    suspend fun writeAutoSave(originalPath: String, content: String) =
        withContext(Dispatchers.IO) {
            val safeName = sanitizeFileName(originalPath)
            val dataFile = File(autoSaveDir, "autosave_${safeName}.tmp")
            val metaFile = File(autoSaveDir, "autosave_${safeName}.meta")

            dataFile.writeText(content, StandardCharsets.UTF_8)
            metaFile.writeText(originalPath, StandardCharsets.UTF_8)
        }

    /**
     * Checks if an auto-save file exists for recovery.
     *
     * @return A [AutoSaveData] if a recoverable auto-save exists, null otherwise.
     */
    suspend fun checkAutoSave(): AutoSaveData? = withContext(Dispatchers.IO) {
        val autoSaveFiles = autoSaveDir.listFiles { _, name ->
            name.startsWith("autosave_") && name.endsWith(".tmp")
        }

        // Return the most recently modified auto-save
        val latestAutoSave = autoSaveFiles
            ?.maxByOrNull { it.lastModified() }
            ?: return@withContext null

        val safeName = latestAutoSave.name
            .removePrefix("autosave_")
            .removeSuffix(".tmp")
        val metaFile = File(autoSaveDir, "autosave_${safeName}.meta")

        val originalPath = if (metaFile.exists()) {
            metaFile.readText(StandardCharsets.UTF_8)
        } else {
            "untitled"
        }

        val content = latestAutoSave.readText(StandardCharsets.UTF_8)

        AutoSaveData(
            originalPath = originalPath,
            content = content,
            timestamp = latestAutoSave.lastModified()
        )
    }

    /**
     * Clears all auto-save files after a successful manual save
     * or when the user dismisses the recovery prompt.
     */
    suspend fun clearAutoSave() = withContext(Dispatchers.IO) {
        autoSaveDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Clears auto-save for a specific file path.
     */
    suspend fun clearAutoSaveForPath(originalPath: String) = withContext(Dispatchers.IO) {
        val safeName = sanitizeFileName(originalPath)
        File(autoSaveDir, "autosave_${safeName}.tmp").delete()
        File(autoSaveDir, "autosave_${safeName}.meta").delete()
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Sanitizes a file path into a safe filename by replacing
     * path separators and special characters with underscores.
     */
    private fun sanitizeFileName(path: String): String {
        return path.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(100) // Limit length to prevent filesystem issues
    }
}

/**
 * Data class representing a recoverable auto-save entry.
 *
 * @property originalPath The original file path being edited.
 * @property content The auto-saved text content.
 * @property timestamp When the auto-save was written (epoch millis).
 */
data class AutoSaveData(
    val originalPath: String,
    val content: String,
    val timestamp: Long
)
