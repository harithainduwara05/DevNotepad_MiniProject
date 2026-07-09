package com.devnotepad.editor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devnotepad.editor.data.local.AutoSaveData
import com.devnotepad.editor.data.model.EditorState
import com.devnotepad.editor.data.model.RecentFile
import com.devnotepad.editor.data.model.SyntaxDetector
import com.devnotepad.editor.data.model.SyntaxMode
import com.devnotepad.editor.data.model.TextEdit
import com.devnotepad.editor.data.model.UndoRedoManager
import com.devnotepad.editor.data.local.entity.DocumentEntity
import com.devnotepad.editor.data.local.entity.VersionSnapshotEntity
import com.devnotepad.editor.data.repository.DocumentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the DevNotepad editor.
 *
 * Responsibilities:
 *  - Manages the canonical [EditorState] observed by the Compose UI.
 *  - Coordinates file operations (open, save, save-as) through the repository.
 *  - Runs the auto-save coroutine (10-second interval) for crash prevention.
 *  - Manages undo/redo stacks via [UndoRedoManager].
 *  - Implements text search and search-and-replace logic.
 *  - Handles read-only mode toggling.
 *  - Checks for crash recovery on launch.
 *  - Triggers version saves and version history queries.
 *
 * All state mutations flow through private MutableStateFlows, exposed
 * as read-only StateFlows to the UI layer.
 */
class EditorViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────

    /** Canonical editor state observed by the Compose UI */
    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    /** Undo/redo manager for the current editing session */
    private val undoRedoManager = UndoRedoManager(maxStackSize = 100)

    /** Whether undo is available (derived state for UI) */
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    /** Whether redo is available (derived state for UI) */
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    /** Recent files list for the sidebar, derived from Room observer */
    private val _recentFiles = MutableStateFlow<List<RecentFile>>(emptyList())
    val recentFiles: StateFlow<List<RecentFile>> = _recentFiles.asStateFlow()

    /** Auto-save recovery data found at launch (null if no recovery needed) */
    private val _recoveryData = MutableStateFlow<AutoSaveData?>(null)
    val recoveryData: StateFlow<AutoSaveData?> = _recoveryData.asStateFlow()

    /** Version history for the currently open document */
    private val _versionHistory = MutableStateFlow<List<VersionSnapshotEntity>>(emptyList())
    val versionHistory: StateFlow<List<VersionSnapshotEntity>> = _versionHistory.asStateFlow()

    /** Snackbar / toast messages for transient UI feedback */
    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // ─────────────────────────────────────────────────────────────────
    // Internal tracking
    // ─────────────────────────────────────────────────────────────────

    /** Reference to the auto-save coroutine job so it can be cancelled/restarted */
    private var autoSaveJob: Job? = null

    /** The last text content that was successfully saved (to detect modifications) */
    private var lastSavedContent: String = ""

    /** Job tracking version history observation (cancelled when switching files) */
    private var versionHistoryJob: Job? = null

    /** Flag to suppress recording undo entries during programmatic text changes */
    private var suppressUndoRecording = false

    // ─────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────

    init {
        // Start observing the recent files list from Room
        observeRecentFiles()

        // Check for crash recovery auto-save on launch
        checkForCrashRecovery()
    }

    /**
     * Observes the documents table in Room and maps entries to
     * [RecentFile] display models for the sidebar.
     */
    private fun observeRecentFiles() {
        viewModelScope.launch {
            repository.observeAllDocuments()
                .map { documents ->
                    documents.map { doc ->
                        RecentFile(
                            documentId = doc.id,
                            name = doc.name,
                            filePath = doc.filePath,
                            lastModifiedAt = doc.lastModifiedAt,
                            versionCount = repository.getVersionCount(doc.id)
                        )
                    }
                }
                .catch { e ->
                    _userMessage.tryEmit("Error loading recent files: ${e.message}")
                }
                .collect { files ->
                    _recentFiles.value = files
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // File Operations
    // ─────────────────────────────────────────────────────────────────

    /**
     * Opens a file from the given path.
     *
     * Flow:
     *  1. Set loading state.
     *  2. Read file contents via repository.
     *  3. Register/lookup the document in Room.
     *  4. Detect syntax mode from file extension.
     *  5. Reset undo/redo stacks.
     *  6. Start auto-save timer.
     *  7. Start observing version history for this document.
     *
     * @param path Absolute file path to open.
     */
    fun openFile(path: String) {
        viewModelScope.launch {
            try {
                _editorState.update { it.copy(isLoading = true, errorMessage = null) }

                // Read file content
                val content = repository.openFile(path)
                val fileName = repository.extractFileName(path)
                val syntaxMode = SyntaxDetector.detectFromPath(path)

                // Register in Room (get existing or create new)
                val document = repository.getOrCreateDocument(path, fileName)

                // Get current version number
                val latestVersion = repository.getLatestVersionNumber(document.id)

                // Reset editor state for the new file
                lastSavedContent = content
                undoRedoManager.clear()
                updateUndoRedoState()

                _editorState.update {
                    EditorState(
                        currentFilePath = path,
                        fileName = fileName,
                        content = content,
                        isModified = false,
                        isReadOnly = false,
                        documentId = document.id,
                        currentVersion = latestVersion,
                        syntaxMode = syntaxMode,
                        wordWrapEnabled = it.wordWrapEnabled, // Preserve user preference
                        isLoading = false
                    )
                }

                // Clear any auto-save for this file (it's now properly open)
                repository.clearAutoSaveForPath(path)

                // Start background auto-save
                startAutoSave()

                // Observe version history for this document
                observeVersionHistory(document.id)

                _userMessage.tryEmit("Opened: $fileName")

            } catch (e: Exception) {
                _editorState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to open file: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Creates a new untitled file.
     *
     * Resets the editor to a blank state without any file path.
     * The file must be saved with "Save As" before version tracking begins.
     */
    fun newFile() {
        // Cancel existing auto-save and version observation
        autoSaveJob?.cancel()
        versionHistoryJob?.cancel()

        lastSavedContent = ""
        undoRedoManager.clear()
        updateUndoRedoState()
        _versionHistory.value = emptyList()

        _editorState.value = EditorState(
            wordWrapEnabled = _editorState.value.wordWrapEnabled // Preserve preference
        )

        // Start auto-save even for untitled files (crash recovery)
        startAutoSave()
    }

    /**
     * Saves the current file to its existing path.
     *
     * If the file has no path (untitled), this is a no-op — the UI
     * should call [saveFileAs] instead.
     *
     * Flow:
     *  1. Write content to the filesystem.
     *  2. Create a new version snapshot (delta-based).
     *  3. Update the "last saved" reference.
     *  4. Clear auto-save cache.
     */
    fun saveFile() {
        val state = _editorState.value
        val path = state.currentFilePath ?: return // No path → need Save As
        val documentId = state.documentId ?: return

        viewModelScope.launch {
            try {
                _editorState.update { it.copy(isLoading = true) }

                // Write to filesystem
                repository.saveFile(path, state.content)

                // Create version snapshot (delta-based)
                val snapshot = repository.saveVersion(
                    documentId = documentId,
                    currentText = state.content,
                    description = null // User can add descriptions via version history UI
                )

                // Update tracking state
                lastSavedContent = state.content
                _editorState.update {
                    it.copy(
                        isModified = false,
                        isLoading = false,
                        currentVersion = snapshot.versionNumber
                    )
                }

                // Clear auto-save (file is now safely saved)
                repository.clearAutoSaveForPath(path)

                _userMessage.tryEmit("Saved: ${state.fileName} (v${snapshot.versionNumber})")

            } catch (e: Exception) {
                _editorState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Save failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Saves the current content to a new file path ("Save As").
     *
     * Creates a new document entry in Room and saves version 1.
     *
     * @param path The new absolute file path to save to.
     */
    fun saveFileAs(path: String) {
        val state = _editorState.value

        viewModelScope.launch {
            try {
                _editorState.update { it.copy(isLoading = true) }

                val fileName = repository.extractFileName(path)
                val syntaxMode = SyntaxDetector.detectFromPath(path)

                // Write to filesystem
                repository.saveFile(path, state.content)

                // Create new document entry in Room
                val document = repository.getOrCreateDocument(path, fileName)

                // Save version 1 (full text)
                val snapshot = repository.saveVersion(
                    documentId = document.id,
                    currentText = state.content,
                    description = "Initial save"
                )

                // Update editor state with new path/document info
                lastSavedContent = state.content
                _editorState.update {
                    it.copy(
                        currentFilePath = path,
                        fileName = fileName,
                        documentId = document.id,
                        currentVersion = snapshot.versionNumber,
                        syntaxMode = syntaxMode,
                        isModified = false,
                        isLoading = false
                    )
                }

                // Clear old auto-save, start fresh
                repository.clearAutoSave()
                startAutoSave()

                // Observe version history for the new document
                observeVersionHistory(document.id)

                _userMessage.tryEmit("Saved as: $fileName")

            } catch (e: Exception) {
                _editorState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Save As failed: ${e.message}"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Text Editing & Content Updates
    // ─────────────────────────────────────────────────────────────────

    /**
     * Called by the Compose TextField whenever the user types or deletes text.
     *
     * Records the edit in the undo stack (unless suppressed) and updates
     * the editor state. The modified flag is set by comparing against
     * the last saved content.
     *
     * @param newText The new full text content from the TextField.
     */
    fun onTextChanged(newText: String) {
        val currentState = _editorState.value

        // Ignore changes in read-only mode
        if (currentState.isReadOnly) return

        // Record undo entry (unless this is a programmatic change)
        if (!suppressUndoRecording && newText != currentState.content) {
            undoRedoManager.recordEdit(
                TextEdit(
                    oldText = currentState.content,
                    newText = newText
                )
            )
            updateUndoRedoState()
        }

        // Update state
        _editorState.update {
            it.copy(
                content = newText,
                isModified = newText != lastSavedContent,
                // Update search results if search is active
                searchResultCount = if (it.isSearchVisible && it.searchQuery.isNotEmpty()) {
                    countOccurrences(newText, it.searchQuery)
                } else {
                    it.searchResultCount
                }
            )
        }
    }

    /**
     * Performs an undo operation.
     *
     * Restores the previous text from the undo stack without creating
     * a new undo entry (suppressed recording).
     */
    fun undo() {
        val edit = undoRedoManager.undo() ?: return

        suppressUndoRecording = true
        _editorState.update {
            it.copy(
                content = edit.oldText,
                isModified = edit.oldText != lastSavedContent
            )
        }
        suppressUndoRecording = false
        updateUndoRedoState()
    }

    /**
     * Performs a redo operation.
     *
     * Reapplies the next text from the redo stack without creating
     * a new undo entry (suppressed recording).
     */
    fun redo() {
        val edit = undoRedoManager.redo() ?: return

        suppressUndoRecording = true
        _editorState.update {
            it.copy(
                content = edit.newText,
                isModified = edit.newText != lastSavedContent
            )
        }
        suppressUndoRecording = false
        updateUndoRedoState()
    }

    /**
     * Updates the canUndo/canRedo state flows from the manager.
     */
    private fun updateUndoRedoState() {
        _canUndo.value = undoRedoManager.canUndo
        _canRedo.value = undoRedoManager.canRedo
    }

    // ─────────────────────────────────────────────────────────────────
    // Search & Replace
    // ─────────────────────────────────────────────────────────────────

    /**
     * Toggles the search bar visibility.
     * When hiding, clears search state.
     */
    fun toggleSearch() {
        _editorState.update {
            if (it.isSearchVisible) {
                // Hiding search — clear search state
                it.copy(
                    isSearchVisible = false,
                    searchQuery = "",
                    replaceQuery = "",
                    searchResultCount = 0,
                    currentSearchIndex = 0
                )
            } else {
                it.copy(isSearchVisible = true)
            }
        }
    }

    /**
     * Updates the search query and recalculates match count.
     *
     * @param query The search text entered by the user.
     */
    fun onSearchQueryChanged(query: String) {
        val content = _editorState.value.content
        val count = if (query.isNotEmpty()) countOccurrences(content, query) else 0

        _editorState.update {
            it.copy(
                searchQuery = query,
                searchResultCount = count,
                currentSearchIndex = if (count > 0) 0 else 0
            )
        }
    }

    /**
     * Updates the replacement text for search-and-replace.
     *
     * @param query The replacement text.
     */
    fun onReplaceQueryChanged(query: String) {
        _editorState.update { it.copy(replaceQuery = query) }
    }

    /**
     * Moves to the next search result.
     */
    fun findNext() {
        _editorState.update {
            if (it.searchResultCount > 0) {
                it.copy(
                    currentSearchIndex = (it.currentSearchIndex + 1) % it.searchResultCount
                )
            } else it
        }
    }

    /**
     * Moves to the previous search result.
     */
    fun findPrevious() {
        _editorState.update {
            if (it.searchResultCount > 0) {
                it.copy(
                    currentSearchIndex = if (it.currentSearchIndex > 0) {
                        it.currentSearchIndex - 1
                    } else {
                        it.searchResultCount - 1
                    }
                )
            } else it
        }
    }

    /**
     * Replaces the current search match with the replacement text.
     *
     * Finds the Nth occurrence (based on currentSearchIndex) and replaces it.
     */
    fun replaceCurrent() {
        val state = _editorState.value
        if (state.isReadOnly || state.searchQuery.isEmpty()) return

        val content = state.content
        val query = state.searchQuery
        val replacement = state.replaceQuery

        // Find the Nth occurrence
        val index = findNthOccurrence(content, query, state.currentSearchIndex)
        if (index == -1) return

        // Build new content with the replacement
        val newContent = buildString {
            append(content.substring(0, index))
            append(replacement)
            append(content.substring(index + query.length))
        }

        onTextChanged(newContent)

        // Recalculate search results
        val newCount = countOccurrences(newContent, query)
        _editorState.update {
            it.copy(
                searchResultCount = newCount,
                currentSearchIndex = if (newCount > 0) {
                    it.currentSearchIndex.coerceAtMost(newCount - 1)
                } else 0
            )
        }
    }

    /**
     * Replaces all occurrences of the search query with the replacement text.
     */
    fun replaceAll() {
        val state = _editorState.value
        if (state.isReadOnly || state.searchQuery.isEmpty()) return

        val newContent = state.content.replace(state.searchQuery, state.replaceQuery)
        onTextChanged(newContent)

        _editorState.update {
            it.copy(
                searchResultCount = 0,
                currentSearchIndex = 0
            )
        }

        val replacements = countOccurrences(state.content, state.searchQuery)
        _userMessage.tryEmit("Replaced $replacements occurrence(s)")
    }

    /**
     * Counts the number of non-overlapping occurrences of [query] in [text].
     */
    private fun countOccurrences(text: String, query: String): Int {
        if (query.isEmpty()) return 0
        var count = 0
        var startIndex = 0
        while (true) {
            val index = text.indexOf(query, startIndex, ignoreCase = false)
            if (index == -1) break
            count++
            startIndex = index + query.length
        }
        return count
    }

    /**
     * Finds the character index of the Nth (0-based) occurrence of [query] in [text].
     *
     * @return The starting character index, or -1 if not found.
     */
    private fun findNthOccurrence(text: String, query: String, n: Int): Int {
        var count = 0
        var startIndex = 0
        while (true) {
            val index = text.indexOf(query, startIndex, ignoreCase = false)
            if (index == -1) return -1
            if (count == n) return index
            count++
            startIndex = index + query.length
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Editor Toggles
    // ─────────────────────────────────────────────────────────────────

    /**
     * Toggles read-only mode.
     * When enabled, text input is disabled and the UI shows a lock indicator.
     */
    fun toggleReadOnly() {
        _editorState.update { it.copy(isReadOnly = !it.isReadOnly) }
        val mode = if (_editorState.value.isReadOnly) "ON" else "OFF"
        _userMessage.tryEmit("Read-only mode: $mode")
    }

    /**
     * Toggles word wrap in the editor.
     */
    fun toggleWordWrap() {
        _editorState.update { it.copy(wordWrapEnabled = !it.wordWrapEnabled) }
    }

    /**
     * Changes the syntax highlighting mode.
     *
     * @param mode The new [SyntaxMode] to apply.
     */
    fun setSyntaxMode(mode: SyntaxMode) {
        _editorState.update { it.copy(syntaxMode = mode) }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _editorState.update { it.copy(errorMessage = null) }
    }

    // ─────────────────────────────────────────────────────────────────
    // Auto-Save (Crash Prevention)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Starts the auto-save background coroutine.
     *
     * Writes the current editor buffer to a temporary cache file every
     * 10 seconds. This ensures that if the app crashes or is killed by
     * the system, the user's work can be recovered on next launch.
     *
     * The coroutine is automatically cancelled when the ViewModel is
     * cleared, or when a new file is opened (restarted).
     */
    private fun startAutoSave() {
        // Cancel any existing auto-save job
        autoSaveJob?.cancel()

        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_SAVE_INTERVAL_MS)

                val state = _editorState.value
                // Only auto-save if there's content and it's been modified
                if (state.content.isNotEmpty() && state.isModified) {
                    try {
                        val path = state.currentFilePath ?: "untitled"
                        repository.writeAutoSave(path, state.content)
                    } catch (e: Exception) {
                        // Auto-save failures are silent — don't disrupt the user
                        // but log for debugging
                    }
                }
            }
        }
    }

    /**
     * Checks for crash recovery data on app launch.
     *
     * If a previous auto-save exists, populates [_recoveryData] so the
     * UI can show a recovery prompt dialog.
     */
    private fun checkForCrashRecovery() {
        viewModelScope.launch {
            try {
                val recovery = repository.checkAutoSave()
                if (recovery != null) {
                    _recoveryData.value = recovery
                }
            } catch (e: Exception) {
                // If recovery check fails, silently continue
            }
        }
    }

    /**
     * User chose to restore the auto-saved content from a crash.
     *
     * Loads the recovered text into the editor and sets appropriate state.
     */
    fun restoreFromAutoSave() {
        val recovery = _recoveryData.value ?: return

        viewModelScope.launch {
            val isUntitled = recovery.originalPath == "untitled"

            if (isUntitled) {
                // Recovered an untitled file — load content without a file path
                _editorState.update {
                    it.copy(
                        content = recovery.content,
                        isModified = true,
                        fileName = "Untitled (Recovered)"
                    )
                }
            } else {
                // Recovered a named file — try to open it first, then overlay content
                try {
                    // Load the file metadata
                    val fileName = repository.extractFileName(recovery.originalPath)
                    val syntaxMode = SyntaxDetector.detectFromPath(recovery.originalPath)
                    val document = repository.getOrCreateDocument(
                        recovery.originalPath, fileName
                    )

                    _editorState.update {
                        EditorState(
                            currentFilePath = recovery.originalPath,
                            fileName = "$fileName (Recovered)",
                            content = recovery.content,
                            isModified = true, // Mark as modified since it may differ from disk
                            documentId = document.id,
                            syntaxMode = syntaxMode,
                            wordWrapEnabled = it.wordWrapEnabled
                        )
                    }

                    observeVersionHistory(document.id)
                } catch (e: Exception) {
                    // File may have been deleted — still load the content
                    _editorState.update {
                        it.copy(
                            content = recovery.content,
                            isModified = true,
                            fileName = "Recovered"
                        )
                    }
                }
            }

            lastSavedContent = "" // Force "modified" detection
            undoRedoManager.clear()
            updateUndoRedoState()
            startAutoSave()

            // Clear recovery data (user has accepted the restore)
            _recoveryData.value = null
            _userMessage.tryEmit("Session restored from auto-save")
        }
    }

    /**
     * User chose to discard the auto-saved recovery.
     *
     * Clears the recovery data and deletes the auto-save files.
     */
    fun dismissAutoSave() {
        viewModelScope.launch {
            repository.clearAutoSave()
            _recoveryData.value = null
            startAutoSave() // Start fresh auto-save for new session
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Version Control
    // ─────────────────────────────────────────────────────────────────

    /**
     * Starts observing version history for a document.
     * Cancels any previous observation (when switching files).
     */
    private fun observeVersionHistory(documentId: Long) {
        versionHistoryJob?.cancel()
        versionHistoryJob = viewModelScope.launch {
            repository.observeVersionHistory(documentId)
                .catch { e ->
                    _userMessage.tryEmit("Error loading version history: ${e.message}")
                }
                .collect { snapshots ->
                    _versionHistory.value = snapshots
                }
        }
    }

    /**
     * Reconstructs the text at a specific version for preview.
     *
     * @param versionNumber The version to reconstruct.
     * @param onResult Callback with the reconstructed text.
     */
    fun previewVersion(versionNumber: Int, onResult: (String) -> Unit) {
        val documentId = _editorState.value.documentId ?: return

        viewModelScope.launch {
            try {
                val text = repository.reconstructVersion(documentId, versionNumber)
                onResult(text)
            } catch (e: Exception) {
                _userMessage.tryEmit("Failed to reconstruct version: ${e.message}")
            }
        }
    }

    /**
     * Rolls back the document to a specific version.
     *
     * Reconstructs the text at the target version, deletes all newer
     * versions, and updates the editor with the restored text.
     *
     * @param targetVersion The version number to roll back to.
     */
    fun rollbackToVersion(targetVersion: Int) {
        val documentId = _editorState.value.documentId ?: return

        viewModelScope.launch {
            try {
                _editorState.update { it.copy(isLoading = true) }

                val restoredText = repository.rollbackToVersion(documentId, targetVersion)

                lastSavedContent = restoredText
                undoRedoManager.clear()
                updateUndoRedoState()

                _editorState.update {
                    it.copy(
                        content = restoredText,
                        isModified = false,
                        isLoading = false,
                        currentVersion = targetVersion
                    )
                }

                _userMessage.tryEmit("Rolled back to version $targetVersion")

            } catch (e: Exception) {
                _editorState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Rollback failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Computes a line diff between two versions for the Diff View UI.
     *
     * @param fromVersion The older version number.
     * @param toVersion The newer version number.
     * @param onResult Callback with the list of DiffLine entries.
     */
    fun computeDiff(
        fromVersion: Int,
        toVersion: Int,
        onResult: (List<com.devnotepad.editor.data.local.DiffLine>) -> Unit
    ) {
        val documentId = _editorState.value.documentId ?: return

        viewModelScope.launch {
            try {
                val diffLines = repository.computeDiffBetweenVersions(
                    documentId, fromVersion, toVersion
                )
                onResult(diffLines)
            } catch (e: Exception) {
                _userMessage.tryEmit("Failed to compute diff: ${e.message}")
            }
        }
    }

    /**
     * Manually triggers a version save with an optional description.
     *
     * Unlike [saveFile], this creates a version snapshot without
     * necessarily writing to the filesystem (useful for "checkpoint" saves).
     *
     * @param description Optional commit message for this version.
     */
    fun createVersionSnapshot(description: String? = null) {
        val state = _editorState.value
        val documentId = state.documentId ?: return

        viewModelScope.launch {
            try {
                val snapshot = repository.saveVersion(
                    documentId = documentId,
                    currentText = state.content,
                    description = description
                )
                _editorState.update { it.copy(currentVersion = snapshot.versionNumber) }
                _userMessage.tryEmit("Version ${snapshot.versionNumber} saved")
            } catch (e: Exception) {
                _userMessage.tryEmit("Failed to create snapshot: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Recent Files (Sidebar)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Opens a recent file from the sidebar.
     *
     * @param recentFile The [RecentFile] entry selected by the user.
     */
    fun openRecentFile(recentFile: RecentFile) {
        openFile(recentFile.filePath)
    }

    /**
     * Removes a document from the recent files list and deletes
     * all its version history.
     *
     * @param recentFile The [RecentFile] to remove.
     */
    fun removeRecentFile(recentFile: RecentFile) {
        viewModelScope.launch {
            try {
                val document = repository.findDocumentById(recentFile.documentId)
                if (document != null) {
                    repository.deleteDocument(document)
                    _userMessage.tryEmit("Removed: ${recentFile.name}")
                }
            } catch (e: Exception) {
                _userMessage.tryEmit("Failed to remove: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SAF (Storage Access Framework) Integration
    // ─────────────────────────────────────────────────────────────────

    /**
     * Called when a file is opened via the Android file picker (SAF).
     *
     * Unlike [openFile], this receives the content directly (already read
     * from the ContentResolver in the UI layer) rather than reading from
     * a filesystem path.
     *
     * @param uriString The URI string used as the document "path".
     * @param displayName The display name extracted from the URI.
     * @param content The file content already read via ContentResolver.
     */
    fun onFileOpenedFromPicker(uriString: String, displayName: String, content: String) {
        viewModelScope.launch {
            try {
                _editorState.update { it.copy(isLoading = true, errorMessage = null) }

                val actualDisplayName = repository.extractFileName(uriString)
                val syntaxMode = SyntaxDetector.detectFromName(actualDisplayName)
                val document = repository.getOrCreateDocument(uriString, actualDisplayName)
                val latestVersion = repository.getLatestVersionNumber(document.id)

                lastSavedContent = content
                undoRedoManager.clear()
                updateUndoRedoState()

                _editorState.update {
                    EditorState(
                        currentFilePath = uriString,
                        fileName = actualDisplayName,
                        content = content,
                        isModified = false,
                        isReadOnly = false,
                        documentId = document.id,
                        currentVersion = latestVersion,
                        syntaxMode = syntaxMode,
                        wordWrapEnabled = it.wordWrapEnabled,
                        isLoading = false
                    )
                }

                repository.clearAutoSaveForPath(uriString)
                startAutoSave()
                observeVersionHistory(document.id)

                _userMessage.tryEmit("Opened: $displayName")
            } catch (e: Exception) {
                _editorState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to open: ${e.message}")
                }
            }
        }
    }

    /**
     * Called when a file is saved via the Android file picker (Save As via SAF).
     *
     * The actual content writing to the URI is handled by the UI layer
     * via ContentResolver. This method updates the ViewModel state and
     * creates the initial version snapshot.
     *
     * @param uriString The URI string for the new file location.
     * @param displayName The display name for the new file.
     */
    fun onFileSavedFromPicker(uriString: String, displayName: String) {
        viewModelScope.launch {
            try {
                _editorState.update { it.copy(isLoading = true) }

                val state = _editorState.value
                val actualDisplayName = repository.extractFileName(uriString)
                val syntaxMode = SyntaxDetector.detectFromName(actualDisplayName)

                // Write content via repository (to the URI path)
                repository.saveFile(uriString, state.content)

                val document = repository.getOrCreateDocument(uriString, actualDisplayName)
                val snapshot = repository.saveVersion(
                    documentId = document.id,
                    currentText = state.content,
                    description = "Initial save"
                )

                lastSavedContent = state.content
                _editorState.update {
                    it.copy(
                        currentFilePath = uriString,
                        fileName = actualDisplayName,
                        documentId = document.id,
                        currentVersion = snapshot.versionNumber,
                        syntaxMode = syntaxMode,
                        isModified = false,
                        isLoading = false
                    )
                }

                repository.clearAutoSave()
                startAutoSave()
                observeVersionHistory(document.id)

                _userMessage.tryEmit("Saved as: $displayName")
            } catch (e: Exception) {
                _editorState.update {
                    it.copy(isLoading = false, errorMessage = "Save As failed: ${e.message}")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        versionHistoryJob?.cancel()
    }

    companion object {
        /** Auto-save interval: 10 seconds */
        const val AUTO_SAVE_INTERVAL_MS = 10_000L
    }
}
