package com.devnotepad.editor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devnotepad.editor.data.local.DiffEngine
import com.devnotepad.editor.data.local.FileManager
import com.devnotepad.editor.data.local.DevNotepadDatabase
import com.devnotepad.editor.data.repository.DocumentRepository
import com.devnotepad.editor.DevNotepadApp

/**
 * Factory for creating [EditorViewModel] instances with proper dependencies.
 *
 * Since we're not using a DI framework (Hilt/Dagger), this factory
 * manually constructs the dependency graph:
 *
 *   DevNotepadApp
 *       │
 *       ├── DevNotepadDatabase
 *       │       ├── DocumentDao
 *       │       └── VersionSnapshotDao
 *       │
 *       ├── FileManager(context)
 *       │
 *       └── DiffEngine()
 *             │
 *             ▼
 *       DocumentRepository(dao, dao, fileManager, diffEngine)
 *             │
 *             ▼
 *       EditorViewModel(repository)
 *
 * Usage in Compose:
 * ```
 * val viewModel: EditorViewModel = viewModel(
 *     factory = EditorViewModelFactory()
 * )
 * ```
 */
class EditorViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            val app = DevNotepadApp.instance
            val database = app.database

            // Build the dependency graph
            val fileManager = FileManager(app.applicationContext)
            val diffEngine = DiffEngine()
            val repository = DocumentRepository(
                documentDao = database.documentDao(),
                versionSnapshotDao = database.versionSnapshotDao(),
                fileManager = fileManager,
                diffEngine = diffEngine
            )

            return EditorViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}. " +
            "EditorViewModelFactory only creates EditorViewModel."
        )
    }
}
