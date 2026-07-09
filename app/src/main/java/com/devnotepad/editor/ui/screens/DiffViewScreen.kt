package com.devnotepad.editor.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devnotepad.editor.data.local.DiffLine
import com.devnotepad.editor.data.local.DiffLineType
import com.devnotepad.editor.ui.viewmodel.VersionHistoryViewModel
import com.devnotepad.editor.ui.viewmodel.VersionHistoryViewModelFactory

/**
 * Diff View screen — displays a line-by-line comparison between two
 * versions of a document.
 *
 * Features:
 *  - Color-coded lines: green for additions, red for deletions, neutral for unchanged
 *  - Line numbers from both the old and new versions
 *  - Summary statistics (additions, deletions, unchanged)
 *  - Monospace rendering for proper character alignment
 *  - Horizontal scrolling for long lines
 *
 * Visual layout for each line:
 * ```
 * ┌─────┬─────┬───┬──────────────────────────────────┐
 * │ Old │ New │ ± │ Line content                     │
 * │  42 │  43 │   │   val x = 10                     │  ← Equal (neutral)
 * │  43 │     │ - │   val y = 20                     │  ← Deleted (red)
 * │     │  44 │ + │   val y = 30                     │  ← Added (green)
 * └─────┴─────┴───┴──────────────────────────────────┘
 * ```
 *
 * @param documentId The Room ID of the document.
 * @param fromVersion The older version number.
 * @param toVersion The newer version number.
 * @param onNavigateBack Navigate back to the version history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffViewScreen(
    documentId: Long,
    fromVersion: Int,
    toVersion: Int,
    onNavigateBack: () -> Unit
) {
    val historyViewModel: VersionHistoryViewModel = viewModel(
        factory = VersionHistoryViewModelFactory(documentId)
    )

    val diffLines by historyViewModel.diffLines.collectAsState()
    val isDiffLoading by historyViewModel.isDiffLoading.collectAsState()

    // Compute the diff on first composition
    LaunchedEffect(fromVersion, toVersion) {
        historyViewModel.computeDiff(fromVersion, toVersion)
    }

    // Calculate statistics
    val stats = remember(diffLines) {
        DiffStats(
            additions = diffLines.count { it.type == DiffLineType.INSERT },
            deletions = diffLines.count { it.type == DiffLineType.DELETE },
            unchanged = diffLines.count { it.type == DiffLineType.EQUAL }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Diff View",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "v$fromVersion → v$toVersion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Stats bar at the bottom
            if (diffLines.isNotEmpty()) {
                DiffStatsBar(stats = stats)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isDiffLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Computing diff...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                diffLines.isEmpty() && !isDiffLoading -> {
                    // No differences
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No differences",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Versions $fromVersion and $toVersion are identical",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.6f
                                )
                            )
                        }
                    }
                }

                else -> {
                    // Diff content
                    DiffContent(
                        diffLines = diffLines,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diff Content
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the diff content as a lazy list of color-coded lines.
 */
@Composable
private fun DiffContent(
    diffLines: List<DiffLine>,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()

    // Calculate max line number width for alignment
    val maxOldLine = diffLines.maxOfOrNull { it.oldLineNumber ?: 0 } ?: 0
    val maxNewLine = diffLines.maxOfOrNull { it.newLineNumber ?: 0 } ?: 0
    val lineNumDigits = maxOf(maxOldLine, maxNewLine).toString().length.coerceAtLeast(2)
    val lineNumWidth = (lineNumDigits * 9 + 8).dp

    LazyColumn(
        modifier = modifier
    ) {
        // Header row
        item {
            DiffHeaderRow(lineNumWidth = lineNumWidth)
        }

        // Diff lines
        itemsIndexed(diffLines, key = { index, _ -> index }) { _, line ->
            DiffLineRow(
                diffLine = line,
                lineNumWidth = lineNumWidth,
                lineNumDigits = lineNumDigits,
                horizontalScrollState = horizontalScrollState
            )
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Header row for the diff view showing column labels.
 */
@Composable
private fun DiffHeaderRow(lineNumWidth: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Old line number header
        Text(
            text = "Old",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(lineNumWidth)
                .padding(start = 4.dp)
        )

        // New line number header
        Text(
            text = "New",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(lineNumWidth)
        )

        // Marker column header
        Text(
            text = " ",
            modifier = Modifier.width(20.dp)
        )

        // Content header
        Text(
            text = "Content",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    HorizontalDivider(thickness = 1.dp)
}

/**
 * Renders a single line of the diff with appropriate color coding.
 *
 * Color scheme:
 *  - EQUAL: neutral background, normal text
 *  - INSERT: green-tinted background, "+" marker
 *  - DELETE: red-tinted background, "−" marker
 */
@Composable
private fun DiffLineRow(
    diffLine: DiffLine,
    lineNumWidth: Dp,
    lineNumDigits: Int,
    horizontalScrollState: androidx.compose.foundation.ScrollState
) {
    // Colors based on diff type
    val backgroundColor = when (diffLine.type) {
        DiffLineType.INSERT -> DiffColors.insertBackground
        DiffLineType.DELETE -> DiffColors.deleteBackground
        DiffLineType.EQUAL -> Color.Transparent
    }

    val markerColor = when (diffLine.type) {
        DiffLineType.INSERT -> DiffColors.insertMarker
        DiffLineType.DELETE -> DiffColors.deleteMarker
        DiffLineType.EQUAL -> Color.Transparent
    }

    val textColor = when (diffLine.type) {
        DiffLineType.INSERT -> DiffColors.insertText
        DiffLineType.DELETE -> DiffColors.deleteText
        DiffLineType.EQUAL -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    }

    val markerChar = when (diffLine.type) {
        DiffLineType.INSERT -> "+"
        DiffLineType.DELETE -> "−"
        DiffLineType.EQUAL -> " "
    }

    val lineNumberColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .heightIn(min = 22.dp),
        verticalAlignment = Alignment.Top
    ) {
        // ── Old line number ──
        Text(
            text = diffLine.oldLineNumber
                ?.toString()
                ?.padStart(lineNumDigits)
                ?: " ".repeat(lineNumDigits),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            ),
            color = lineNumberColor,
            modifier = Modifier
                .width(lineNumWidth)
                .padding(start = 4.dp, top = 2.dp)
        )

        // ── New line number ──
        Text(
            text = diffLine.newLineNumber
                ?.toString()
                ?.padStart(lineNumDigits)
                ?: " ".repeat(lineNumDigits),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            ),
            color = lineNumberColor,
            modifier = Modifier
                .width(lineNumWidth)
                .padding(top = 2.dp)
        )

        // ── Change marker (+/-) ──
        Box(
            modifier = Modifier
                .width(20.dp)
                .padding(top = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (diffLine.type != DiffLineType.EQUAL) {
                Text(
                    text = markerChar,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = markerColor
                )
            }
        }

        // ── Vertical separator ──
        Box(
            modifier = Modifier
                .width(1.dp)
                .heightIn(min = 22.dp)
                .background(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
        )

        // ── Line content ──
        Text(
            text = diffLine.content,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            ),
            color = textColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 2.dp, end = 8.dp, bottom = 2.dp)
                .horizontalScroll(horizontalScrollState),
            softWrap = false
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats Bar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Bottom statistics bar showing the count of additions, deletions,
 * and unchanged lines.
 */
@Composable
private fun DiffStatsBar(
    stats: DiffStats,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Additions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DiffColors.insertMarker)
                )
                Text(
                    text = "+${stats.additions} added",
                    style = MaterialTheme.typography.labelMedium,
                    color = DiffColors.insertMarker,
                    fontWeight = FontWeight.Medium
                )
            }

            // Deletions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DiffColors.deleteMarker)
                )
                Text(
                    text = "−${stats.deletions} removed",
                    style = MaterialTheme.typography.labelMedium,
                    color = DiffColors.deleteMarker,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Unchanged
            Text(
                text = "${stats.unchanged} unchanged",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            // Total
            Text(
                text = "${stats.total} lines",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data & Colors
// ─────────────────────────────────────────────────────────────────────────────

private data class DiffStats(
    val additions: Int,
    val deletions: Int,
    val unchanged: Int
) {
    val total: Int get() = additions + deletions + unchanged
}

/**
 * Color palette for diff line rendering.
 * Designed for dark themes with subtle backgrounds and vivid markers.
 */
private object DiffColors {
    // Insertion (green tones)
    val insertBackground = Color(0xFF0D2818)  // Very dark green background
    val insertMarker = Color(0xFF4ADE80)      // Bright green for + marker
    val insertText = Color(0xFFA7F3D0)        // Light green for text

    // Deletion (red tones)
    val deleteBackground = Color(0xFF2D0F0F)  // Very dark red background
    val deleteMarker = Color(0xFFF87171)      // Bright red for − marker
    val deleteText = Color(0xFFFECACA)        // Light red for text
}
