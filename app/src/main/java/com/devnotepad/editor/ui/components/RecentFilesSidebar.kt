package com.devnotepad.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devnotepad.editor.data.model.RecentFile
import java.text.SimpleDateFormat
import java.util.*

/**
 * Navigation drawer content showing the list of recent files.
 *
 * Features:
 *  - App branding header
 *  - "New File" action button
 *  - List of recent files with name, path, last modified time, and version count
 *  - Swipe-to-delete or long-press menu for removing files from history
 *  - Currently active file is highlighted
 *
 * @param recentFiles List of recent file entries from Room.
 * @param currentFilePath Path of the currently open file (for highlighting).
 * @param onFileSelected Callback when a recent file is tapped.
 * @param onNewFile Callback to create a new file.
 * @param onRemoveFile Callback to remove a file from the recent list.
 * @param onOpenFile Callback to open a file picker.
 */
@Composable
fun RecentFilesSidebar(
    recentFiles: List<RecentFile>,
    currentFilePath: String?,
    onFileSelected: (RecentFile) -> Unit,
    onNewFile: () -> Unit,
    onRemoveFile: (RecentFile) -> Unit,
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        // ── Header ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App icon placeholder
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "DevNotepad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Developer Text Editor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Action Buttons ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // New File button
            FilledTonalButton(
                onClick = onNewFile,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Filled.NoteAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("New", style = MaterialTheme.typography.labelMedium)
            }

            // Open File button
            FilledTonalButton(
                onClick = onOpenFile,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Filled.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // ── Section Header ──
        Text(
            text = "RECENT FILES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // ── Recent Files List ──
        if (recentFiles.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No recent files",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(recentFiles, key = { it.documentId }) { file ->
                    RecentFileItem(
                        file = file,
                        isActive = file.filePath == currentFilePath,
                        onClick = { onFileSelected(file) },
                        onRemove = { onRemoveFile(file) }
                    )
                }
            }
        }
    }
}

/**
 * Individual recent file item in the sidebar.
 */
@Composable
private fun RecentFileItem(
    file: RecentFile,
    isActive: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    ListItem(
        headlineContent = {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(file.lastModifiedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (file.versionCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "${file.versionCount}v",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        },
        leadingContent = {
            val icon = getFileIcon(file.name)
            Icon(
                icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        },
        trailingContent = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove from recent",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(
                if (isActive) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                } else Modifier
            ),
        colors = ListItemDefaults.colors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    )
}

/**
 * Returns an appropriate icon based on the file extension.
 */
private fun getFileIcon(fileName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> Icons.Filled.Code
        fileName.endsWith(".md") || fileName.endsWith(".markdown") -> Icons.Filled.Description
        fileName.endsWith(".json") -> Icons.Filled.DataObject
        fileName.endsWith(".xml") -> Icons.Filled.Code
        fileName.endsWith(".txt") -> Icons.Filled.TextSnippet
        fileName.endsWith(".log") -> Icons.Filled.Terminal
        else -> Icons.Filled.InsertDriveFile
    }
}
