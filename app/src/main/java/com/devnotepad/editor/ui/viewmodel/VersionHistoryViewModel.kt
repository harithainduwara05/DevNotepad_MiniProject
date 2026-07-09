package com.devnotepad.editor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devnotepad.editor.DevNotepadApp
import com.devnotepad.editor.data.local.DiffEngine
import com.devnotepad.editor.data.local.DiffLine
import com.devnotepad.editor.data.local.FileManager
import com.devnotepad.editor.data.local.entity.VersionSnapshotEntity
import com.devnotepad.editor.data.repository.DocumentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Version History and Diff View screens.
 *
 * Separated from [EditorViewModel] to keep concerns clean:
 *  - EditorViewModel handles editing and file operations.
 *  - VersionHistoryViewModel handles browsing and comparing versions.
 *
 * @param repository The document repository for version queries.
 * @param documentId The Room ID of the document to show history for.
 */
class VersionHistoryViewModel(
    private val repository: DocumentRepository,
    private val documentId: Long
) : ViewModel() {

    /** List of all version snapshots for this document, ordered by version number */
    private val _snapshots = MutableStateFlow<List<VersionSnapshotEntity>>(emptyList())
    val snapshots: StateFlow<List<VersionSnapshotEntity>> = _snapshots.asStateFlow()

    /** Whether the version list is loading */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Reconstructed text preview for a selected version */
    private val _previewText = MutableStateFlow<String?>(null)
    val previewText: StateFlow<String?> = _previewText.asStateFlow()

    /** The version number currently being previewed */
    private val _previewVersion = MutableStateFlow<Int?>(null)
    val previewVersion: StateFlow<Int?> = _previewVersion.asStateFlow()

    /** Line diff results for the diff view */
    private val _diffLines = MutableStateFlow<List<DiffLine>>(emptyList())
    val diffLines: StateFlow<List<DiffLine>> = _diffLines.asStateFlow()

    /** Whether a diff is currently being computed */
    private val _isDiffLoading = MutableStateFlow(false)
    val isDiffLoading: StateFlow<Boolean> = _isDiffLoading.asStateFlow()

    /** Error/info messages */
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        loadSnapshots()
    }

    /**
     * Loads all version snapshots for the document.
     */
    private fun loadSnapshots() {
        viewModelScope.launch {
            repository.observeVersionHistory(documentId)
                .catch { e ->
                    _message.tryEmit("Error loading history: ${e.message}")
                    _isLoading.value = false
                }
                .collect { snapshots ->
                    _snapshots.value = snapshots
                    _isLoading.value = false
                }
        }
    }

    /**
     * Reconstructs and previews the text at a specific version.
     *
     * @param versionNumber The version to preview.
     */
    fun previewVersion(versionNumber: Int) {
        viewModelScope.launch {
            try {
                _previewVersion.value = versionNumber
                val text = repository.reconstructVersion(documentId, versionNumber)
                _previewText.value = text
            } catch (e: Exception) {
                _message.tryEmit("Failed to preview version $versionNumber: ${e.message}")
                _previewText.value = null
                _previewVersion.value = null
            }
        }
    }

    /**
     * Clears the version preview.
     */
    fun clearPreview() {
        _previewText.value = null
        _previewVersion.value = null
    }

    /**
     * Computes a line-by-line diff between two versions.
     *
     * @param fromVersion The older version.
     * @param toVersion The newer version.
     */
    fun computeDiff(fromVersion: Int, toVersion: Int) {
        viewModelScope.launch {
            try {
                _isDiffLoading.value = true
                val lines = repository.computeDiffBetweenVersions(
                    documentId, fromVersion, toVersion
                )
                _diffLines.value = lines
                _isDiffLoading.value = false
            } catch (e: Exception) {
                _isDiffLoading.value = false
                _message.tryEmit("Failed to compute diff: ${e.message}")
            }
        }
    }

    /**
     * Clears the diff lines (when navigating away from diff view).
     */
    fun clearDiff() {
        _diffLines.value = emptyList()
    }
}

/**
 * Factory for creating [VersionHistoryViewModel] with the required
 * document ID parameter.
 *
 * @param documentId The Room ID of the document to show history for.
 */
class VersionHistoryViewModelFactory(
    private val documentId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VersionHistoryViewModel::class.java)) {
            val app = DevNotepadApp.instance
            val database = app.database
            val repository = DocumentRepository(
                documentDao = database.documentDao(),
                versionSnapshotDao = database.versionSnapshotDao(),
                fileManager = FileManager(app.applicationContext),
                diffEngine = DiffEngine()
            )
            return VersionHistoryViewModel(repository, documentId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
