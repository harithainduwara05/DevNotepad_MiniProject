package com.devnotepad.editor.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import com.devnotepad.editor.data.model.SyntaxMode
import com.devnotepad.editor.ui.components.*

import com.devnotepad.editor.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Main Editor screen — the primary UI of the app.
 *
 * Assembles:
 *  - [EditorToolbar] — Top action bar
 *  - [SearchReplaceBar] — Expandable search/replace (below toolbar)
 *  - [EditorTextField] — Code editor with line numbers & syntax highlighting
 *  - [EditorStatusBar] — Bottom info bar
 *  - [RecentFilesSidebar] — Navigation drawer (left)
 *  - [CrashRecoveryDialog] — Auto-save recovery prompt
 *
 * @param viewModel The [EditorViewModel] managing all editor state.
 * @param onNavigateToVersionHistory Navigate to version history screen.
 * @param onNavigateToMarkdownPreview Navigate to Markdown preview screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateToVersionHistory: (Long) -> Unit,
    onNavigateToMarkdownPreview: () -> Unit
) {
    val editorState by viewModel.editorState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val recoveryData by viewModel.recoveryData.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ── Snackbar messages ──
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // ── File picker launcher (Open) ──
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleOpenFile(context as Activity, it, viewModel) }
    }

    // ── File picker launcher (Save As) ──
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/*")
    ) { uri: Uri? ->
        uri?.let { handleSaveAs(context as Activity, it, viewModel) }
    }



    // ── Crash Recovery Dialog ──
    recoveryData?.let { recovery ->
        CrashRecoveryDialog(
            originalPath = recovery.originalPath,
            timestamp = recovery.timestamp,
            onRestore = { viewModel.restoreFromAutoSave() },
            onDiscard = { viewModel.dismissAutoSave() }
        )
    }

    // ── Navigation Drawer ──
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            RecentFilesSidebar(
                recentFiles = recentFiles,
                currentFilePath = editorState.currentFilePath,
                onFileSelected = { file ->
                    viewModel.openRecentFile(file)
                    scope.launch { drawerState.close() }
                },
                onNewFile = {
                    viewModel.newFile()
                    scope.launch { drawerState.close() }
                },
                onRemoveFile = { file ->
                    viewModel.removeRecentFile(file)
                },
                onOpenFile = {
                    scope.launch { drawerState.close() }
                    openFileLauncher.launch(arrayOf("text/*", "*/*"))
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    // ── Toolbar ──
                    EditorToolbar(
                        fileName = editorState.fileName,
                        isModified = editorState.isModified,
                        isReadOnly = editorState.isReadOnly,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        syntaxMode = editorState.syntaxMode,
                        wordWrapEnabled = editorState.wordWrapEnabled,
                        currentVersion = editorState.currentVersion,
                        hasDocument = editorState.documentId != null,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onNewFile = { viewModel.newFile() },
                        onOpenFile = {
                            openFileLauncher.launch(arrayOf("text/*", "*/*"))
                        },
                        onSaveFile = {
                            if (editorState.currentFilePath != null) {
                                viewModel.saveFile()
                            } else {
                                saveAsLauncher.launch(editorState.fileName)
                            }
                        },
                        onSaveFileAs = {
                            saveAsLauncher.launch(editorState.fileName)
                        },
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onToggleSearch = { viewModel.toggleSearch() },
                        onToggleReadOnly = { viewModel.toggleReadOnly() },
                        onToggleWordWrap = { viewModel.toggleWordWrap() },
                        onSetSyntaxMode = { viewModel.setSyntaxMode(it) },
                        onVersionHistory = {
                            editorState.documentId?.let { docId ->
                                onNavigateToVersionHistory(docId)
                            }
                        },
                        onMarkdownPreview = onNavigateToMarkdownPreview
                    )

                    // ── Search/Replace Bar (animated) ──
                    AnimatedVisibility(
                        visible = editorState.isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        SearchReplaceBar(
                            searchQuery = editorState.searchQuery,
                            replaceQuery = editorState.replaceQuery,
                            searchResultCount = editorState.searchResultCount,
                            currentSearchIndex = editorState.currentSearchIndex,
                            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                            onReplaceQueryChanged = { viewModel.onReplaceQueryChanged(it) },
                            onFindNext = { viewModel.findNext() },
                            onFindPrevious = { viewModel.findPrevious() },
                            onReplaceCurrent = { viewModel.replaceCurrent() },
                            onReplaceAll = { viewModel.replaceAll() },
                            onClose = { viewModel.toggleSearch() }
                        )
                    }
                }
            },
            bottomBar = {
                // ── Status Bar ──
                EditorStatusBar(
                    lineCount = if (editorState.content.isEmpty()) 0
                    else editorState.content.count { it == '\n' } + 1,
                    charCount = editorState.content.length,
                    syntaxMode = editorState.syntaxMode.displayName,
                    isReadOnly = editorState.isReadOnly
                )
            }
        ) { innerPadding ->
            // ── Editor ──
            Box(modifier = Modifier.padding(innerPadding)) {
                if (editorState.isLoading) {
                    // Loading overlay
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    EditorTextField(
                        content = editorState.content,
                        onContentChanged = { viewModel.onTextChanged(it) },
                        isReadOnly = editorState.isReadOnly,
                        wordWrapEnabled = editorState.wordWrapEnabled,
                        syntaxMode = editorState.syntaxMode
                    )
                }
            }
        }
    }

    // ── Error Dialog ──
    editorState.errorMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// File Picker Helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Handles the result of the Open File picker.
 *
 * Uses ContentResolver to read the file content from a SAF Uri,
 * then delegates to the ViewModel. Persists URI permission for
 * future access.
 */
private fun handleOpenFile(activity: Activity, uri: Uri, viewModel: EditorViewModel) {
    try {
        // Take persistable permission for future access
        activity.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // Not all URIs support persistable permissions — safe to ignore
    }

    // Read content from the Uri
    val content = try {
        activity.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
        } ?: ""
    } catch (e: Exception) {
        ""
    }

    // Extract a display name from the Uri
    val displayName = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.substringAfterLast(':')
        ?: "Unknown"

    // Use the URI string as the "path" since SAF doesn't expose real paths
    viewModel.onFileOpenedFromPicker(uri.toString(), displayName, content)
}

/**
 * Handles the result of the Save As file picker.
 *
 * Writes the current editor content to the selected URI location.
 */
private fun handleSaveAs(activity: Activity, uri: Uri, viewModel: EditorViewModel) {
    try {
        activity.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    } catch (_: SecurityException) { }

    val displayName = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.substringAfterLast(':')
        ?: "Unknown"

    viewModel.onFileSavedFromPicker(uri.toString(), displayName)
}
