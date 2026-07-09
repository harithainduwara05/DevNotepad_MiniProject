package com.devnotepad.editor.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devnotepad.editor.data.local.entity.VersionSnapshotEntity
import com.devnotepad.editor.ui.viewmodel.EditorViewModel
import com.devnotepad.editor.ui.viewmodel.VersionHistoryViewModel
import com.devnotepad.editor.ui.viewmodel.VersionHistoryViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Version History screen — lists all saved versions (snapshots) of the
 * current document with timestamps, version numbers, and descriptions.
 *
 * Features:
 *  - Chronological timeline of all versions (newest first)
 *  - Visual timeline connector between version cards
 *  - Tap a version to preview its reconstructed content
 *  - "Restore" button to roll back to any previous version
 *  - "Compare" to open the diff view between two versions
 *  - Version 1 is marked as "Initial" with a distinct badge
 *
 * @param documentId The Room ID of the document to show history for.
 * @param editorViewModel The shared editor ViewModel for rollback actions.
 * @param onNavigateBack Navigate back to the editor screen.
 * @param onNavigateToDiff Navigate to the diff view for two versions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryScreen(
    documentId: Long,
    editorViewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDiff: (fromVersion: Int, toVersion: Int) -> Unit
) {
    // Separate ViewModel for version history browsing
    val historyViewModel: VersionHistoryViewModel = viewModel(
        factory = VersionHistoryViewModelFactory(documentId)
    )

    val snapshots by historyViewModel.snapshots.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()
    val previewText by historyViewModel.previewText.collectAsState()
    val previewVersion by historyViewModel.previewVersion.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Collect messages
    LaunchedEffect(Unit) {
        historyViewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    // State for compare mode (selecting two versions to diff)
    var compareMode by remember { mutableStateOf(false) }
    var selectedVersionA by remember { mutableIntStateOf(-1) }
    var selectedVersionB by remember { mutableIntStateOf(-1) }

    // State for rollback confirmation dialog
    var rollbackTarget by remember { mutableIntStateOf(-1) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Version History",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${snapshots.size} version${if (snapshots.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to editor"
                        )
                    }
                },
                actions = {
                    // Compare mode toggle
                    if (snapshots.size >= 2) {
                        FilledTonalIconToggleButton(
                            checked = compareMode,
                            onCheckedChange = {
                                compareMode = it
                                if (!it) {
                                    selectedVersionA = -1
                                    selectedVersionB = -1
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Compare,
                                contentDescription = "Compare versions",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        // Compare FAB — shown when two versions are selected
        floatingActionButton = {
            if (compareMode && selectedVersionA >= 0 && selectedVersionB >= 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val from = minOf(selectedVersionA, selectedVersionB)
                        val to = maxOf(selectedVersionA, selectedVersionB)
                        onNavigateToDiff(from, to)
                    },
                    icon = {
                        Icon(Icons.Filled.Compare, contentDescription = null)
                    },
                    text = {
                        Text("Compare v$selectedVersionA → v$selectedVersionB")
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                snapshots.isEmpty() -> {
                    // Empty state
                    EmptyVersionHistory()
                }

                else -> {
                    // Version list (newest first)
                    val reversedSnapshots = snapshots.reversed()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        itemsIndexed(
                            reversedSnapshots,
                            key = { _, s -> s.id }
                        ) { index, snapshot ->
                            VersionTimelineItem(
                                snapshot = snapshot,
                                isLatest = index == 0,
                                isFirst = index == reversedSnapshots.size - 1,
                                isCompareMode = compareMode,
                                isSelectedA = snapshot.versionNumber == selectedVersionA,
                                isSelectedB = snapshot.versionNumber == selectedVersionB,
                                onPreview = {
                                    historyViewModel.previewVersion(snapshot.versionNumber)
                                },
                                onRestore = {
                                    rollbackTarget = snapshot.versionNumber
                                },
                                onCompareSelect = {
                                    when {
                                        selectedVersionA < 0 -> {
                                            selectedVersionA = snapshot.versionNumber
                                        }
                                        selectedVersionB < 0 &&
                                                snapshot.versionNumber != selectedVersionA -> {
                                            selectedVersionB = snapshot.versionNumber
                                        }
                                        snapshot.versionNumber == selectedVersionA -> {
                                            selectedVersionA = selectedVersionB
                                            selectedVersionB = -1
                                        }
                                        snapshot.versionNumber == selectedVersionB -> {
                                            selectedVersionB = -1
                                        }
                                        else -> {
                                            selectedVersionB = snapshot.versionNumber
                                        }
                                    }
                                },
                                onDiffWithPrevious = {
                                    if (snapshot.versionNumber > 1) {
                                        onNavigateToDiff(
                                            snapshot.versionNumber - 1,
                                            snapshot.versionNumber
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ── Preview Dialog ──
            previewText?.let { text ->
                VersionPreviewDialog(
                    versionNumber = previewVersion ?: 0,
                    content = text,
                    onDismiss = { historyViewModel.clearPreview() }
                )
            }

            // ── Rollback Confirmation Dialog ──
            if (rollbackTarget > 0) {
                RollbackConfirmDialog(
                    targetVersion = rollbackTarget,
                    currentVersion = snapshots.lastOrNull()?.versionNumber ?: 0,
                    onConfirm = {
                        editorViewModel.rollbackToVersion(rollbackTarget)
                        rollbackTarget = -1
                        onNavigateBack()
                    },
                    onDismiss = { rollbackTarget = -1 }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Timeline Item
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single version entry in the timeline list.
 *
 * Visual structure:
 * ```
 *  ●──┐  v3 · Jul 2, 14:30             [Latest]
 *  │  │  Optional description here
 *  │  │  [Preview]  [Restore]  [Diff ▵]
 *  │──┘
 * ```
 */
@Composable
private fun VersionTimelineItem(
    snapshot: VersionSnapshotEntity,
    isLatest: Boolean,
    isFirst: Boolean,
    isCompareMode: Boolean,
    isSelectedA: Boolean,
    isSelectedB: Boolean,
    onPreview: () -> Unit,
    onRestore: () -> Unit,
    onCompareSelect: () -> Unit,
    onDiffWithPrevious: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val isSelected = isSelectedA || isSelectedB

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Timeline connector ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            // Top connector line (hidden for the first/latest item)
            if (!isLatest) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Timeline dot
            val dotColor = when {
                isLatest -> MaterialTheme.colorScheme.primary
                isFirst -> MaterialTheme.colorScheme.tertiary
                isSelected -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outlineVariant
            }

            Box(
                modifier = Modifier
                    .size(if (isLatest || isFirst) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            // Bottom connector line (hidden for the last/oldest item)
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        // ── Version Card ──
        val cardColor = when {
            isSelectedA -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isSelectedB -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            isLatest -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
                .then(
                    if (isCompareMode) Modifier.clickable { onCompareSelect() }
                    else Modifier
                ),
            shape = RoundedCornerShape(12.dp),
            color = cardColor,
            tonalElevation = if (isLatest) 2.dp else 0.dp,
            border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isSelectedA) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            } else null
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // ── Header Row: Version + Timestamp + Badge ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "v${snapshot.versionNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "· ${dateFormat.format(Date(snapshot.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isLatest) {
                            VersionBadge(
                                text = "Latest",
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        if (isFirst) {
                            VersionBadge(
                                text = "Initial",
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                        if (isSelectedA) {
                            VersionBadge(
                                text = "A",
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        if (isSelectedB) {
                            VersionBadge(
                                text = "B",
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }

                // ── Description ──
                snapshot.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ── Action Buttons (hidden in compare mode) ──
                if (!isCompareMode) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Preview
                        AssistChip(
                            onClick = onPreview,
                            label = {
                                Text("Preview", style = MaterialTheme.typography.labelSmall)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.height(30.dp)
                        )

                        // Restore (not for latest version)
                        if (!isLatest) {
                            AssistChip(
                                onClick = onRestore,
                                label = {
                                    Text("Restore", style = MaterialTheme.typography.labelSmall)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = MaterialTheme.colorScheme.primary,
                                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.height(30.dp)
                            )
                        }

                        // Diff with previous (not for v1)
                        if (snapshot.versionNumber > 1) {
                            AssistChip(
                                onClick = onDiffWithPrevious,
                                label = {
                                    Text(
                                        "Diff ▵",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Difference,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Supporting Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VersionBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyVersionHistory() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Text(
                text = "No versions saved yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Save the file to create the first version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Full-screen dialog showing the reconstructed text at a specific version.
 */
@Composable
private fun VersionPreviewDialog(
    versionNumber: Int,
    content: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Preview — Version $versionNumber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close preview"
                        )
                    }
                }

                HorizontalDivider()

                // Content — scrollable monospace text
                val lineCount = content.count { it == '\n' } + 1
                val gutterWidth = (lineCount.toString().length * 10 + 16).dp

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Line numbers
                    val lineNumbers = buildString {
                        for (i in 1..lineCount) {
                            if (i > 1) append('\n')
                            append(i.toString().padStart(lineCount.toString().length))
                        }
                    }

                    Text(
                        text = lineNumbers,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .width(gutterWidth)
                            .padding(start = 8.dp, top = 8.dp, end = 4.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    )

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                    )

                    // File content
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    )
                }

                // Footer with stats
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Lines: $lineCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Chars: ${content.length}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Confirmation dialog before rolling back to a previous version.
 * Warns that all versions after the target will be permanently deleted.
 */
@Composable
private fun RollbackConfirmDialog(
    targetVersion: Int,
    currentVersion: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val versionsToDelete = currentVersion - targetVersion

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Restore to Version $targetVersion?",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This will restore the file content to version $targetVersion.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "$versionsToDelete version${if (versionsToDelete != 1) "s" else ""} " +
                                    "(v${targetVersion + 1}–v$currentVersion) will be permanently deleted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
