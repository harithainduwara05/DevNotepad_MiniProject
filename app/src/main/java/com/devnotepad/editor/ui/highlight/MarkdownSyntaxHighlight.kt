package com.devnotepad.editor.ui.highlight

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.devnotepad.editor.ui.theme.SyntaxColors

/**
 * Custom [VisualTransformation] that applies Markdown syntax highlighting
 * to the editor's BasicTextField.
 *
 * Highlights the following Markdown elements:
 *  - **Headings** (# H1, ## H2, etc.) — purple, bold, sized
 *  - **Bold text** (**bold** or __bold__) — bright white, bold
 *  - **Italic text** (*italic* or _italic_) — lighter purple, italic
 *  - **Inline code** (`code`) — green, monospace
 *  - **Code blocks** (``` ... ```) — green
 *  - **Links** ([text](url)) — blue
 *  - **List markers** (- or * or 1.) — amber
 *  - **Blockquotes** (> text) — gray, italic
 *  - **Horizontal rules** (--- or ***) — gray
 *
 * Like [KotlinSyntaxHighlight], code blocks/inline code are claimed first
 * so their contents are not re-highlighted as bold/italic.
 */
class MarkdownSyntaxHighlight : VisualTransformation {

    companion object {
        /** Code blocks: ``` ... ``` (multiline) */
        private val CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```")

        /** Inline code: `...` (single backtick, no newlines) */
        private val INLINE_CODE_REGEX = Regex("`[^`\n]+`")

        /** Headings: # through ###### at start of line */
        private val HEADING_REGEX = Regex("^#{1,6}\\s+.*$", RegexOption.MULTILINE)

        /** Bold: **text** or __text__ */
        private val BOLD_REGEX = Regex("\\*\\*[^*]+\\*\\*|__[^_]+__")

        /** Italic: *text* or _text_ (not inside bold markers) */
        private val ITALIC_REGEX = Regex("(?<!\\*)\\*(?!\\*)[^*\n]+(?<!\\*)\\*(?!\\*)|(?<!_)_(?!_)[^_\n]+(?<!_)_(?!_)")

        /** Links: [text](url) */
        private val LINK_REGEX = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")

        /** Images: ![alt](url) */
        private val IMAGE_REGEX = Regex("!\\[([^\\]]*)]\\(([^)]+)\\)")

        /** Unordered list markers: - or * at start of line */
        private val UNORDERED_LIST_REGEX = Regex("^\\s*[*\\-+]\\s", RegexOption.MULTILINE)

        /** Ordered list markers: 1. 2. etc. at start of line */
        private val ORDERED_LIST_REGEX = Regex("^\\s*\\d+\\.\\s", RegexOption.MULTILINE)

        /** Blockquotes: > at start of line */
        private val BLOCKQUOTE_REGEX = Regex("^>\\s?.*$", RegexOption.MULTILINE)

        /** Horizontal rules: --- or *** or ___ (3 or more) */
        private val HR_REGEX = Regex("^[-*_]{3,}\\s*$", RegexOption.MULTILINE)

        /** Strikethrough: ~~text~~ */
        private val STRIKETHROUGH_REGEX = Regex("~~[^~]+~~")
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val src = text.text
        val builder = AnnotatedString.Builder(src)

        val claimed = mutableListOf<IntRange>()

        // ── Pass 1: Code (highest priority — contents not re-highlighted) ──

        applyPattern(builder, src, CODE_BLOCK_REGEX, SyntaxColors.mdCode, claimed,
            claim = true)
        applyPattern(builder, src, INLINE_CODE_REGEX, SyntaxColors.mdCode, claimed,
            claim = true)

        // ── Pass 2: Headings ──

        HEADING_REGEX.findAll(src).forEach { match ->
            if (!isInClaimed(match.range, claimed)) {
                // Count # characters to determine heading level
                val level = match.value.trimStart().takeWhile { it == '#' }.length
                val headingWeight = when (level) {
                    1 -> FontWeight.ExtraBold
                    2 -> FontWeight.Bold
                    else -> FontWeight.SemiBold
                }
                builder.addStyle(
                    SpanStyle(
                        color = SyntaxColors.mdHeading,
                        fontWeight = headingWeight
                    ),
                    match.range.first,
                    match.range.last + 1
                )
                claimed.add(match.range)
            }
        }

        // ── Pass 3: Bold and Italic ──

        applyPattern(builder, src, BOLD_REGEX, SyntaxColors.mdBold, claimed,
            fontWeight = FontWeight.Bold)

        applyPattern(builder, src, ITALIC_REGEX, SyntaxColors.mdItalic, claimed,
            fontStyle = FontStyle.Italic)

        applyPattern(builder, src, STRIKETHROUGH_REGEX, Color(0xFF6B7280), claimed)

        // ── Pass 4: Links and Images ──

        // Images (before links, since image syntax contains link syntax)
        IMAGE_REGEX.findAll(src).forEach { match ->
            if (!isInClaimed(match.range, claimed)) {
                builder.addStyle(
                    SpanStyle(color = SyntaxColors.mdLink, fontStyle = FontStyle.Italic),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        // Links: highlight the full [text](url)
        LINK_REGEX.findAll(src).forEach { match ->
            if (!isInClaimed(match.range, claimed)) {
                // Color the link text part
                val textGroup = match.groups[1]
                if (textGroup != null) {
                    builder.addStyle(
                        SpanStyle(color = SyntaxColors.mdLink, fontWeight = FontWeight.Medium),
                        textGroup.range.first,
                        textGroup.range.last + 1
                    )
                }
                // Color the URL part in a dimmer shade
                val urlGroup = match.groups[2]
                if (urlGroup != null) {
                    builder.addStyle(
                        SpanStyle(color = Color(0xFF6B7280)),
                        urlGroup.range.first,
                        urlGroup.range.last + 1
                    )
                }
            }
        }

        // ── Pass 5: Lists, Blockquotes, HRs ──

        applyPattern(builder, src, UNORDERED_LIST_REGEX, SyntaxColors.mdListMarker, claimed,
            fontWeight = FontWeight.Bold)
        applyPattern(builder, src, ORDERED_LIST_REGEX, SyntaxColors.mdListMarker, claimed,
            fontWeight = FontWeight.Bold)

        applyPattern(builder, src, BLOCKQUOTE_REGEX, Color(0xFF9CA3AF), claimed,
            fontStyle = FontStyle.Italic)

        applyPattern(builder, src, HR_REGEX, Color(0xFF6B7280), claimed)

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    /** Applies a SpanStyle for all non-claimed matches of a pattern. */
    private fun applyPattern(
        builder: AnnotatedString.Builder,
        source: String,
        pattern: Regex,
        color: Color,
        claimed: MutableList<IntRange>,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        claim: Boolean = false
    ) {
        pattern.findAll(source).forEach { match ->
            if (!isInClaimed(match.range, claimed)) {
                builder.addStyle(
                    SpanStyle(color = color, fontWeight = fontWeight, fontStyle = fontStyle),
                    match.range.first,
                    match.range.last + 1
                )
                if (claim) claimed.add(match.range)
            }
        }
    }

    /** Checks if a range overlaps any claimed range. */
    private fun isInClaimed(range: IntRange, claimed: List<IntRange>): Boolean {
        return claimed.any { it.first <= range.first && it.last >= range.last }
    }
}
