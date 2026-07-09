package com.devnotepad.editor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog shown on app launch if an auto-save recovery file is found.
 *
 * Indicates a previous crash or unexpected kill, and offers the user
 * the choice to restore the unsaved session or discard it.
 *
 * @param originalPath The file path of the recovered content.
 * @param timestamp When the auto-save was written.
 * @param onRestore User chose to restore the recovered content.
 * @param onDiscard User chose to discard and start fresh.
 */
@Composable
fun CrashRecoveryDialog(
    originalPath: String,
    timestamp: Long,
    onRestore: () -> Unit,
    onDiscard: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy  HH:mm:ss", Locale.getDefault())
    val timeString = dateFormat.format(Date(timestamp))

    AlertDialog(
        onDismissRequest = { /* Don't allow dismiss without a choice */ },
        icon = {
            Icon(
                Icons.Filled.RestorePage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Recover Unsaved Work?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "An auto-saved session was found from a previous session that didn't save properly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // File info card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (originalPath == "untitled") "Untitled file"
                                else originalPath.substringAfterLast('/'),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Last saved: $timeString",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onRestore) {
                Icon(
                    Icons.Filled.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
        }
    )
}

/**
 * A status bar displayed at the bottom of the editor showing
 * line/column info, file encoding, and syntax mode.
 *
 * @param lineCount Total number of lines in the editor.
 * @param charCount Total character count.
 * @param syntaxMode Current syntax highlighting mode display name.
 * @param encoding File encoding (always UTF-8 for now).
 * @param isReadOnly Whether the file is locked.
 */
@Composable
fun EditorStatusBar(
    lineCount: Int,
    charCount: Int,
    syntaxMode: String,
    encoding: String = "UTF-8",
    isReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Line count
            StatusBarItem(label = "Lines", value = lineCount.toString())

            // Character count
            StatusBarItem(label = "Chars", value = charCount.toString())

            Spacer(modifier = Modifier.weight(1f))

            // Read-only indicator
            if (isReadOnly) {
                Text(
                    text = "READ-ONLY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Encoding
            Text(
                text = encoding,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Syntax mode
            Text(
                text = syntaxMode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBarItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
