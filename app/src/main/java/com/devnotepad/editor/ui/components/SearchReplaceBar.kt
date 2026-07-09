package com.devnotepad.editor.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Search and Replace bar that slides down from the top of the editor.
 *
 * Features:
 *  - Text search with match count display ("3 of 12")
 *  - Navigate between matches (up/down arrows)
 *  - Expandable replace section
 *  - Replace current / Replace all buttons
 *  - Close button to dismiss
 *
 * @param searchQuery Current search text.
 * @param replaceQuery Current replacement text.
 * @param searchResultCount Total matches found.
 * @param currentSearchIndex Currently highlighted match (0-based).
 * @param onSearchQueryChanged Callback when search text changes.
 * @param onReplaceQueryChanged Callback when replace text changes.
 * @param onFindNext Navigate to next match.
 * @param onFindPrevious Navigate to previous match.
 * @param onReplaceCurrent Replace the current match.
 * @param onReplaceAll Replace all matches.
 * @param onClose Dismiss the search bar.
 */
@Composable
fun SearchReplaceBar(
    searchQuery: String,
    replaceQuery: String,
    searchResultCount: Int,
    currentSearchIndex: Int,
    onSearchQueryChanged: (String) -> Unit,
    onReplaceQueryChanged: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReplace by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ── Search Row ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Toggle replace section
                IconButton(
                    onClick = { showReplace = !showReplace },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (showReplace) Icons.Filled.ExpandLess
                        else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle replace",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Search input
                SearchInputField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = "Search...",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Match count indicator
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = if (searchResultCount > 0) {
                            "${currentSearchIndex + 1} of $searchResultCount"
                        } else {
                            "No results"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (searchResultCount > 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                // Navigation arrows
                IconButton(
                    onClick = onFindPrevious,
                    enabled = searchResultCount > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Previous match",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onFindNext,
                    enabled = searchResultCount > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Next match",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close search",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Replace Row (expandable) ──
            AnimatedVisibility(
                visible = showReplace,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Spacer(modifier = Modifier.width(36.dp)) // Align with search field

                    SearchInputField(
                        value = replaceQuery,
                        onValueChange = onReplaceQueryChanged,
                        placeholder = "Replace with...",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Replace current
                    TextButton(
                        onClick = onReplaceCurrent,
                        enabled = searchResultCount > 0,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Replace", style = MaterialTheme.typography.labelSmall)
                    }

                    // Replace all
                    TextButton(
                        onClick = onReplaceAll,
                        enabled = searchResultCount > 0,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("All", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/**
 * Minimal search input field styled to match the editor's aesthetic.
 */
@Composable
private fun SearchInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val textFieldState = rememberTextFieldState(initialText = value)

    // Sync external value changes into TextFieldState
    LaunchedEffect(value) {
        if (textFieldState.text.toString() != value) {
            textFieldState.edit {
                replace(0, length, value)
            }
        }
    }

    // Sync TextFieldState changes back to the callback
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { newText ->
                if (newText != value) {
                    onValueChange(newText)
                }
            }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (textFieldState.text.isEmpty()) {
            Text(
                text = placeholder,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            )
        }

        BasicTextField(
            state = textFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

