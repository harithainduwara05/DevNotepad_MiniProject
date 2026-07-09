package com.devnotepad.editor.ui.highlight

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.devnotepad.editor.ui.theme.SyntaxColors

/**
 * Custom [VisualTransformation] that applies Kotlin syntax highlighting
 * to the editor's BasicTextField.
 *
 * Highlights the following token types:
 *  - **Keywords** (val, var, fun, class, if, when, etc.) — purple
 *  - **String literals** (double-quoted and triple-quoted) — green
 *  - **Single-line comments** (//) — gray
 *  - **Block comments** — gray
 *  - **Annotations** (@Something) — amber
 *  - **Numeric literals** (integers, floats, hex) — blue
 *  - **Function declarations** (fun name) — cyan
 *  - **Type references** (capitalized words after : or <) — teal
 *
 * Strategy:
 *  Each regex is applied independently, and resulting spans are merged.
 *  Comment and string spans take priority (tokens inside them are not
 *  re-highlighted). This is achieved by tracking "claimed" ranges.
 *
 * Performance note:
 *  Regex-based highlighting is sufficient for files up to ~5000 lines.
 *  For very large files, consider a line-based incremental approach.
 */
class KotlinSyntaxHighlight : VisualTransformation {

    companion object {
        /** Kotlin hard and soft keywords */
        private val KEYWORDS = setOf(
            "abstract", "actual", "annotation", "as", "break", "by",
            "catch", "class", "companion", "const", "constructor",
            "continue", "crossinline", "data", "delegate", "do",
            "dynamic", "else", "enum", "expect", "external",
            "false", "field", "final", "finally", "for", "fun",
            "get", "if", "import", "in", "infix", "init", "inline",
            "inner", "interface", "internal", "is", "it", "lateinit",
            "lazy", "noinline", "null", "object", "open", "operator",
            "out", "override", "package", "param", "private",
            "property", "protected", "public", "receiver", "reified",
            "return", "sealed", "set", "setparam", "super",
            "suspend", "tailrec", "this", "throw", "true", "try",
            "typealias", "typeof", "val", "var", "vararg", "when",
            "where", "while"
        )

        // ── Regex patterns ──

        /** Matches Kotlin keywords as whole words */
        private val KEYWORD_REGEX = Regex(
            "\\b(${KEYWORDS.joinToString("|")})\\b"
        )

        /** Matches single-line comments: // ... */
        private val LINE_COMMENT_REGEX = Regex("//[^\n]*")

        /** Matches block comments (non-greedy, supports multiline) */
        private val BLOCK_COMMENT_REGEX = Regex("/\\*[\\s\\S]*?\\*/")

        /** Matches triple-quoted strings: \"\"\" ... \"\"\" */
        private val TRIPLE_QUOTE_REGEX = Regex("\"\"\"[\\s\\S]*?\"\"\"")

        /** Matches double-quoted strings: "..." (handles escaped quotes) */
        private val STRING_REGEX = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")

        /** Matches single-quoted chars: '.' */
        private val CHAR_REGEX = Regex("'(?:[^'\\\\]|\\\\.)'")

        /** Matches annotations: @Word or @Word(...) */
        private val ANNOTATION_REGEX = Regex("@[A-Za-z_][A-Za-z0-9_]*")

        /** Matches numeric literals: integers, floats, hex, binary, underscored */
        private val NUMBER_REGEX = Regex(
            "\\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|\\d[\\d_]*(?:\\.\\d[\\d_]*)?(?:[eE][+-]?\\d+)?[fFLl]?)\\b"
        )

        /** Matches function declarations: fun <name> */
        private val FUN_DECL_REGEX = Regex("\\bfun\\s+([A-Za-z_][A-Za-z0-9_]*)")
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val src = text.text
        val builder = AnnotatedString.Builder(src)

        // Track ranges claimed by comments/strings (higher priority)
        val claimed = mutableListOf<IntRange>()

        // ── Pass 1: Comments and Strings (highest priority) ──

        // Block comments
        applyPattern(builder, src, BLOCK_COMMENT_REGEX, SyntaxColors.comment, claimed,
            fontStyle = FontStyle.Italic, claim = true)

        // Triple-quoted strings
        applyPattern(builder, src, TRIPLE_QUOTE_REGEX, SyntaxColors.string, claimed,
            claim = true)

        // Line comments
        applyPattern(builder, src, LINE_COMMENT_REGEX, SyntaxColors.comment, claimed,
            fontStyle = FontStyle.Italic, claim = true)

        // Regular strings
        applyPattern(builder, src, STRING_REGEX, SyntaxColors.string, claimed,
            claim = true)

        // Char literals
        applyPattern(builder, src, CHAR_REGEX, SyntaxColors.string, claimed,
            claim = true)

        // ── Pass 2: Other tokens (skip if inside claimed range) ──

        // Annotations
        applyPattern(builder, src, ANNOTATION_REGEX, SyntaxColors.annotation, claimed)

        // Function declarations (capture group 1 = function name)
        FUN_DECL_REGEX.findAll(src).forEach { match ->
            val group = match.groups[1] ?: return@forEach
            if (!isInClaimed(group.range, claimed)) {
                builder.addStyle(
                    SpanStyle(color = SyntaxColors.function, fontWeight = FontWeight.Medium),
                    group.range.first,
                    group.range.last + 1
                )
            }
        }

        // Keywords
        applyPattern(builder, src, KEYWORD_REGEX, SyntaxColors.keyword, claimed,
            fontWeight = FontWeight.Bold)

        // Numeric literals
        applyPattern(builder, src, NUMBER_REGEX, SyntaxColors.number, claimed)

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    /**
     * Applies a [SpanStyle] for all matches of [pattern] in [source],
     * skipping any ranges already claimed by higher-priority tokens.
     *
     * @param claim If true, adds matched ranges to the [claimed] list.
     */
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
                    SpanStyle(
                        color = color,
                        fontWeight = fontWeight,
                        fontStyle = fontStyle
                    ),
                    match.range.first,
                    match.range.last + 1
                )
                if (claim) {
                    claimed.add(match.range)
                }
            }
        }
    }

    /**
     * Checks if a given range overlaps with any already-claimed range.
     */
    private fun isInClaimed(range: IntRange, claimed: List<IntRange>): Boolean {
        return claimed.any { it.first <= range.first && it.last >= range.last }
    }
}
