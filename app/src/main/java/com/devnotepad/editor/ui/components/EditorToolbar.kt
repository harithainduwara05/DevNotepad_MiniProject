package com.devnotepad.editor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devnotepad.editor.data.model.SyntaxMode

/**
 * Top toolbar for the editor, containing:
 *  - Menu (hamburger) button to open the sidebar drawer
 *  - File name display with modification indicator (●)
 *  - Read-only lock icon
 *  - Action buttons: Undo, Redo, Search, Save
 *  - Overflow menu: New, Save As, Word Wrap, Syntax Mode, Version History
 *
 * @param fileName Current file name to display.
 * @param isModified Whether unsaved changes exist (shows ● indicator).
 * @param isReadOnly Whether the editor is in read-only mode.
 * @param canUndo Whether undo is available.
 * @param canRedo Whether redo is available.
 * @param syntaxMode Current syntax highlighting mode.
 * @param wordWrapEnabled Current word wrap state.
 * @param currentVersion Current version number.
 * @param hasDocument Whether a document is loaded (enables version history).
 * @param onMenuClick Open the sidebar drawer.
 * @param onNewFile Create a new file.
 * @param onOpenFile Trigger file open dialog.
 * @param onSaveFile Save the current file.
 * @param onSaveFileAs Trigger save-as dialog.
 * @param onUndo Perform undo.
 * @param onRedo Perform redo.
 * @param onToggleSearch Toggle search bar.
 * @param onToggleReadOnly Toggle read-only mode.
 * @param onToggleWordWrap Toggle word wrap.
 * @param onSetSyntaxMode Change syntax mode.
 * @param onVersionHistory Navigate to version history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorToolbar(
    fileName: String,
    isModified: Boolean,
    isReadOnly: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    syntaxMode: SyntaxMode,
    wordWrapEnabled: Boolean,
    currentVersion: Int,
    hasDocument: Boolean,
    onMenuClick: () -> Unit,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onSaveFileAs: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleReadOnly: () -> Unit,
    onToggleWordWrap: () -> Unit,
    onSetSyntaxMode: (SyntaxMode) -> Unit,
    onVersionHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSyntaxMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Modification indicator dot
                if (isModified) {
                    Text(
                        text = "● ",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // File name
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Read-only lock badge
                if (isReadOnly) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Read-only",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Version badge
                if (currentVersion > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "v$currentVersion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "Open sidebar"
                )
            }
        },
        actions = {
            // Undo
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo"
                )
            }

            // Redo
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo"
                )
            }

            // Search
            IconButton(onClick = onToggleSearch) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search"
                )
            }

            // Save
            IconButton(onClick = onSaveFile) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = "Save"
                )
            }

            // Overflow menu
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    // New file
                    DropdownMenuItem(
                        text = { Text("New File") },
                        onClick = {
                            showOverflowMenu = false
                            onNewFile()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.NoteAdd, contentDescription = null,
                                modifier = Modifier.size(20.dp))
                        }
                    )

                    // Open file
                    DropdownMenuItem(
                        text = { Text("Open File") },
                        onClick = {
                            showOverflowMenu = false
                            onOpenFile()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null,
                                modifier = Modifier.size(20.dp))
                        }
                    )

                    // Save As
                    DropdownMenuItem(
                        text = { Text("Save As...") },
                        onClick = {
                            showOverflowMenu = false
                            onSaveFileAs()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.SaveAs, contentDescription = null,
                                modifier = Modifier.size(20.dp))
                        }
                    )

                    HorizontalDivider()

                    // Read-only toggle
                    DropdownMenuItem(
                        text = {
                            Text(if (isReadOnly) "Unlock Editing" else "Lock (Read-Only)")
                        },
                        onClick = {
                            showOverflowMenu = false
                            onToggleReadOnly()
                        },
                        leadingIcon = {
                            Icon(
                                if (isReadOnly) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    // Word wrap toggle
                    DropdownMenuItem(
                        text = {
                            Text(if (wordWrapEnabled) "Disable Word Wrap" else "Enable Word Wrap")
                        },
                        onClick = {
                            showOverflowMenu = false
                            onToggleWordWrap()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.WrapText, contentDescription = null,
                                modifier = Modifier.size(20.dp))
                        }
                    )

                    HorizontalDivider()

                    // Syntax mode selector
                    DropdownMenuItem(
                        text = { Text("Syntax: ${syntaxMode.displayName}") },
                        onClick = {
                            showSyntaxMenu = true
                            showOverflowMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Code, contentDescription = null,
                                modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            Icon(Icons.Filled.ChevronRight, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                        }
                    )

                    // Version history
                    if (hasDocument) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Version History") },
                            onClick = {
                                showOverflowMenu = false
                                onVersionHistory()
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.History, contentDescription = null,
                                    modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                // Syntax mode sub-menu
                DropdownMenu(
                    expanded = showSyntaxMenu,
                    onDismissRequest = { showSyntaxMenu = false }
                ) {
                    SyntaxMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.displayName,
                                    fontWeight = if (mode == syntaxMode) FontWeight.Bold else null
                                )
                            },
                            onClick = {
                                showSyntaxMenu = false
                                onSetSyntaxMode(mode)
                            },
                            trailingIcon = {
                                if (mode == syntaxMode) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}
