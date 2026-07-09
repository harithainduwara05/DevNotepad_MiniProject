package com.devnotepad.editor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devnotepad.editor.ui.theme.SyntaxColors

/**
 * Markdown Preview screen — renders Markdown content as formatted text.
 *
 * Converts Markdown syntax into styled [AnnotatedString] for display.
 * Supports headings, bold, italic, code, links, lists, blockquotes,
 * and horizontal rules.
 *
 * This is a read-only preview; the user cannot edit from this screen.
 *
 * @param markdownText The raw Markdown source text.
 * @param fileName The file name to display in the toolbar.
 * @param onNavigateBack Navigate back to the editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownPreviewScreen(
    markdownText: String,
    fileName: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Preview: $fileName",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to editor"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            val renderedText = renderMarkdown(markdownText)
            Text(
                text = renderedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 24.sp
                )
            )
        }
    }
}

/**
 * Converts raw Markdown text into a styled [AnnotatedString].
 *
 * Processes the text line-by-line:
 *  - Lines starting with # are rendered as headings with appropriate sizes.
 *  - Lines starting with > are rendered as blockquotes (italic, gray).
 *  - Lines starting with - or * are rendered as bulleted lists.
 *  - Lines of --- or *** are rendered as horizontal rules.
 *  - Inline formatting: **bold**, *italic*, `code`, [text](url).
 *
 * This is a simplified renderer; for full Markdown support, a dedicated
 * library like Markwon would be more appropriate.
 */
private fun renderMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.lines()

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()

            when {
                // ── Headings ──
                trimmed.startsWith("######") -> appendHeading(trimmed.removePrefix("######").trim(), 6)
                trimmed.startsWith("#####") -> appendHeading(trimmed.removePrefix("#####").trim(), 5)
                trimmed.startsWith("####") -> appendHeading(trimmed.removePrefix("####").trim(), 4)
                trimmed.startsWith("###") -> appendHeading(trimmed.removePrefix("###").trim(), 3)
                trimmed.startsWith("##") -> appendHeading(trimmed.removePrefix("##").trim(), 2)
                trimmed.startsWith("#") -> appendHeading(trimmed.removePrefix("#").trim(), 1)

                // ── Horizontal rule ──
                trimmed.matches(Regex("^[-*_]{3,}$")) -> {
                    withStyle(SpanStyle(color = SyntaxColors.comment)) {
                        append("─".repeat(40))
                    }
                }

                // ── Blockquote ──
                trimmed.startsWith(">") -> {
                    withStyle(SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = SyntaxColors.comment
                    )) {
                        append("  ┃ ")
                        appendInlineFormatting(trimmed.removePrefix(">").trim())
                    }
                }

                // ── Unordered list ──
                trimmed.matches(Regex("^[*\\-+]\\s.*")) -> {
                    withStyle(SpanStyle(color = SyntaxColors.mdListMarker, fontWeight = FontWeight.Bold)) {
                        append("  • ")
                    }
                    appendInlineFormatting(trimmed.substring(2))
                }

                // ── Ordered list ──
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val numberPart = trimmed.substringBefore(". ")
                    val textPart = trimmed.substringAfter(". ")
                    withStyle(SpanStyle(color = SyntaxColors.mdListMarker, fontWeight = FontWeight.Bold)) {
                        append("  $numberPart. ")
                    }
                    appendInlineFormatting(textPart)
                }

                // ── Regular paragraph ──
                else -> {
                    appendInlineFormatting(trimmed)
                }
            }

            // Add newline between lines (except the last one)
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

/**
 * Appends a heading line with appropriate size and weight.
 */
private fun AnnotatedString.Builder.appendHeading(text: String, level: Int) {
    val fontSize = when (level) {
        1 -> 28.sp
        2 -> 24.sp
        3 -> 20.sp
        4 -> 18.sp
        5 -> 16.sp
        else -> 14.sp
    }
    val weight = if (level <= 2) FontWeight.ExtraBold else FontWeight.Bold

    withStyle(SpanStyle(
        fontSize = fontSize,
        fontWeight = weight,
        color = SyntaxColors.mdHeading
    )) {
        appendInlineFormatting(text)
    }
}

/**
 * Processes inline Markdown formatting within a line.
 *
 * Handles: **bold**, *italic*, `inline code`, [text](url)
 * Uses a state-machine approach to parse inline tokens.
 */
private fun AnnotatedString.Builder.appendInlineFormatting(text: String) {
    var i = 0
    while (i < text.length) {
        when {
            // ── Bold: **text** ──
            i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }

            // ── Italic: *text* ──
            text[i] == '*' && (i == 0 || text[i - 1] != '*') -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && (end + 1 >= text.length || text[end + 1] != '*')) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            // ── Inline code: `text` ──
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = SyntaxColors.mdCode,
                        background = SyntaxColors.mdCode.copy(alpha = 0.1f)
                    )) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            // ── Link: [text](url) ──
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = text.substring(i + 1, closeBracket)
                        withStyle(SpanStyle(
                            color = SyntaxColors.mdLink,
                            fontWeight = FontWeight.Medium
                        )) {
                            append(linkText)
                        }
                        i = closeParen + 1
                    } else {
                        append(text[i])
                        i++
                    }
                } else {
                    append(text[i])
                    i++
                }
            }

            // ── Regular character ──
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
