package com.devnotepad.editor.data.model

/**
 * Represents the current state of the editor.
 *
 * This is the canonical UI state object observed by the Compose UI.
 * The ViewModel produces instances of this class via StateFlow, and
 * the Compose screens render based on its values.
 *
 * @property currentFilePath Absolute path of the currently open file, or null for untitled.
 * @property fileName Display name shown in the toolbar (e.g., "Main.kt").
 * @property content The raw text content currently in the editor buffer.
 * @property isModified True if the buffer has unsaved changes since last save.
 * @property isReadOnly True if the editor is locked (read-only mode toggle).
 * @property documentId Room database ID of the current document (null if not yet saved).
 * @property currentVersion The current version number in the version control system.
 * @property syntaxMode The active syntax highlighting mode.
 * @property wordWrapEnabled Whether word wrap is enabled in the editor.
 * @property isLoading True while a file I/O or version operation is in progress.
 * @property errorMessage Transient error message to display (null when no error).
 * @property searchQuery The current search text (empty string when search is inactive).
 * @property replaceQuery The current replacement text for search-and-replace.
 * @property searchResultCount Number of matches found for the current search query.
 * @property currentSearchIndex Index of the currently highlighted search match (0-based).
 * @property isSearchVisible Whether the search bar is currently shown.
 */
data class EditorState(
    val currentFilePath: String? = null,
    val fileName: String = "Untitled",
    val content: String = "",
    val isModified: Boolean = false,
    val isReadOnly: Boolean = false,
    val documentId: Long? = null,
    val currentVersion: Int = 0,
    val syntaxMode: SyntaxMode = SyntaxMode.NONE,
    val wordWrapEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val searchResultCount: Int = 0,
    val currentSearchIndex: Int = 0,
    val isSearchVisible: Boolean = false
)

/**
 * Available syntax highlighting modes.
 *
 * Determines which [VisualTransformation] is applied to the editor
 * text field in the Compose UI.
 */
enum class SyntaxMode(val displayName: String, val extensions: List<String>) {
    /** No syntax highlighting */
    NONE("Plain Text", listOf("txt", "log", "csv")),

    /** Kotlin syntax highlighting */
    KOTLIN("Kotlin", listOf("kt", "kts")),

    /** Markdown syntax highlighting */
    MARKDOWN("Markdown", listOf("md", "markdown", "mdx")),
}

/**
 * Represents a single text edit operation for the undo/redo stack.
 *
 * Each edit captures enough information to both undo (restore old)
 * and redo (reapply new) the change.
 *
 * @property oldText The text content before this edit was applied.
 * @property newText The text content after this edit was applied.
 * @property cursorPosition The cursor position after this edit.
 * @property timestamp When the edit occurred (for grouping rapid edits).
 */
data class TextEdit(
    val oldText: String,
    val newText: String,
    val cursorPosition: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages undo and redo stacks for text edits during an active session.
 *
 * Design:
 *  - Each edit pushes the previous state onto the undo stack.
 *  - Undo pops from the undo stack and pushes to the redo stack.
 *  - Redo pops from the redo stack and pushes to the undo stack.
 *  - Any new edit after an undo clears the redo stack (standard behavior).
 *  - Rapid edits within [GROUPING_THRESHOLD_MS] are merged to avoid
 *    creating an undo entry for every keystroke.
 *
 * @property maxStackSize Maximum number of undo/redo entries to retain.
 */
class UndoRedoManager(private val maxStackSize: Int = 100) {

    private val undoStack = ArrayDeque<TextEdit>()
    private val redoStack = ArrayDeque<TextEdit>()

    /** Whether an undo operation is available */
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    /** Whether a redo operation is available */
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Current number of undo entries */
    val undoCount: Int get() = undoStack.size

    /** Current number of redo entries */
    val redoCount: Int get() = redoStack.size

    companion object {
        /**
         * Edits occurring within this window are merged into a single
         * undo entry. Prevents creating one undo step per keystroke.
         */
        const val GROUPING_THRESHOLD_MS = 500L
    }

    /**
     * Records a new text edit.
     *
     * If the edit occurs within [GROUPING_THRESHOLD_MS] of the last edit,
     * the two are merged (the older edit's oldText is kept, the newer
     * edit's newText replaces it). This groups rapid typing into one undo step.
     *
     * @param edit The [TextEdit] to record.
     */
    fun recordEdit(edit: TextEdit) {
        // New edit after undo invalidates the redo history
        redoStack.clear()

        // Try to merge with the last edit if within the grouping window
        if (undoStack.isNotEmpty()) {
            val lastEdit = undoStack.last()
            if (edit.timestamp - lastEdit.timestamp < GROUPING_THRESHOLD_MS) {
                // Merge: keep the original oldText, update to new newText
                undoStack.removeLast()
                undoStack.addLast(
                    TextEdit(
                        oldText = lastEdit.oldText,
                        newText = edit.newText,
                        cursorPosition = edit.cursorPosition,
                        timestamp = edit.timestamp
                    )
                )
                return
            }
        }

        // Add the edit as a new undo entry
        undoStack.addLast(edit)

        // Trim stack if it exceeds the maximum size
        if (undoStack.size > maxStackSize) {
            undoStack.removeFirst()
        }
    }

    /**
     * Performs an undo operation.
     *
     * @return The [TextEdit] that was undone (caller should set the editor
     *         text to [TextEdit.oldText]), or null if the stack is empty.
     */
    fun undo(): TextEdit? {
        if (undoStack.isEmpty()) return null

        val edit = undoStack.removeLast()
        redoStack.addLast(edit)
        return edit
    }

    /**
     * Performs a redo operation.
     *
     * @return The [TextEdit] that was redone (caller should set the editor
     *         text to [TextEdit.newText]), or null if the stack is empty.
     */
    fun redo(): TextEdit? {
        if (redoStack.isEmpty()) return null

        val edit = redoStack.removeLast()
        undoStack.addLast(edit)
        return edit
    }

    /**
     * Clears both undo and redo stacks.
     * Called when opening a new file or after a rollback.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}

/**
 * Data class for a recent file entry displayed in the sidebar.
 *
 * @property documentId The Room database ID.
 * @property name Display name of the file.
 * @property filePath Absolute file path.
 * @property lastModifiedAt Epoch millis of last modification.
 * @property versionCount Total number of versions saved.
 */
data class RecentFile(
    val documentId: Long,
    val name: String,
    val filePath: String,
    val lastModifiedAt: Long,
    val versionCount: Int = 0
)
