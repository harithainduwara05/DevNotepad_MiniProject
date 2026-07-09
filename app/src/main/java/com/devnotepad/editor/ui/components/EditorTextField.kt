package com.devnotepad.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.ui.text.input.VisualTransformation
import com.devnotepad.editor.data.model.SyntaxMode
import com.devnotepad.editor.ui.highlight.KotlinSyntaxHighlight
import com.devnotepad.editor.ui.highlight.MarkdownSyntaxHighlight
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The core editor composable — a code editor text field with line numbers.
 *
 * Features:
 *  - Line number gutter on the left side
 *  - Monospace font for proper character alignment
 *  - Synchronized vertical scrolling between line numbers and text
 *  - Optional horizontal scrolling (when word wrap is off)
 *  - Read-only mode support
 *  - Dynamically adjusting line number gutter width
 *
 * @param content Current text content.
 * @param onContentChanged Callback when the user edits text.
 * @param isReadOnly Whether editing is disabled.
 * @param wordWrapEnabled Whether word wrap is on (disables horizontal scroll).
 * @param modifier Modifier for the container.
 */
@Composable
fun EditorTextField(
    content: String,
    onContentChanged: (String) -> Unit,
    isReadOnly: Boolean,
    wordWrapEnabled: Boolean,
    syntaxMode: SyntaxMode,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Calculate line count for the gutter
    val currentText = content
    val lineCount = remember(currentText) {
        if (currentText.isEmpty()) 1 else currentText.count { it == '\n' } + 1
    }

    // Calculate gutter width based on number of digits
    val gutterWidth = remember(lineCount) {
        val digits = lineCount.toString().length.coerceAtLeast(2)
        (digits * 10 + 24).dp // ~10dp per digit + padding
    }

    // Editor text style — monospace for code alignment
    val editorTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 20.sp
    )

    // Line number text style — same size for alignment
    val lineNumberStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        lineHeight = 20.sp
    )

    Row(modifier = modifier.fillMaxSize()) {
        // ── Line Number Gutter ──
        // Scrolls vertically in sync with the text field
        Box(
            modifier = Modifier
                .width(gutterWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .verticalScroll(verticalScrollState)
                .padding(end = 8.dp, top = 8.dp, start = 8.dp)
        ) {
            // Build line numbers as a single string for performance
            val lineNumbers = remember(lineCount) {
                buildString {
                    for (i in 1..lineCount) {
                        if (i > 1) append('\n')
                        append(i.toString().padStart(lineCount.toString().length))
                    }
                }
            }

            Text(
                text = lineNumbers,
                style = lineNumberStyle
            )
        }

        // ── Divider ──
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        // ── Text Editor ──
        val textModifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .verticalScroll(verticalScrollState)
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)

        // Add horizontal scroll only when word wrap is disabled
        val scrollableModifier = if (!wordWrapEnabled) {
            textModifier.horizontalScroll(horizontalScrollState)
        } else {
            textModifier
        }

        Box(modifier = scrollableModifier) {
            // Placeholder for empty editor
            if (currentText.isEmpty()) {
                Text(
                    text = "Start typing or open a file...",
                    style = editorTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }

            val visualTransformation = remember(syntaxMode) {
                when (syntaxMode) {
                    SyntaxMode.KOTLIN -> KotlinSyntaxHighlight()
                    SyntaxMode.MARKDOWN -> MarkdownSyntaxHighlight()
                    else -> VisualTransformation.None
                }
            }

            BasicTextField(
                value = content,
                onValueChange = onContentChanged,
                readOnly = isReadOnly,
                textStyle = editorTextStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = visualTransformation
            )
        }
    }
}
