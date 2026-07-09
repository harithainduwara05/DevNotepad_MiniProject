package com.devnotepad.editor.data.local

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch
import com.github.difflib.patch.PatchFailedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps the java-diff-utils library to provide diff/patch operations
 * for the delta-based version control system.
 *
 * Core operations:
 *  1. [generatePatch] — Computes the unified diff between two text versions.
 *  2. [applyPatch]    — Applies a unified diff to reconstruct a newer version.
 *  3. [computeLineDiff] — Returns structured line-by-line diff for the Diff View UI.
 *
 * All operations are suspend functions running on [Dispatchers.Default]
 * (CPU-bound work, not I/O).
 */
class DiffEngine {

    /**
     * Generates a unified diff (patch) between the original and revised text.
     *
     * The output is a standard unified diff format string that can be
     * stored in the database or filesystem and later applied with [applyPatch].
     *
     * @param originalText The previous version of the text.
     * @param revisedText The new (current) version of the text.
     * @param fileName Optional label used in the diff header (e.g., "Main.kt").
     * @return A unified diff string, or empty string if texts are identical.
     */
    suspend fun generatePatch(
        originalText: String,
        revisedText: String,
        fileName: String = "file"
    ): String = withContext(Dispatchers.Default) {
        val originalLines = originalText.lines()
        val revisedLines = revisedText.lines()

        // Compute the diff between the two line lists
        val patch: Patch<String> = DiffUtils.diff(originalLines, revisedLines)

        // If there are no deltas, texts are identical — no patch needed
        if (patch.deltas.isEmpty()) {
            return@withContext ""
        }

        // Generate unified diff format with 3 lines of context
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
            "a/$fileName",   // Original file label
            "b/$fileName",   // Revised file label
            originalLines,
            patch,
            3               // Context lines (standard is 3)
        )

        unifiedDiff.joinToString("\n")
    }

    /**
     * Applies a unified diff patch to the original text to produce the revised text.
     *
     * Used during version reconstruction: start from base text (v1) and
     * sequentially apply patches v2, v3, ..., vN.
     *
     * @param originalText The text to apply the patch to.
     * @param patchString The unified diff string to apply.
     * @return The text after applying the patch.
     * @throws PatchFailedException if the patch cannot be cleanly applied.
     */
    suspend fun applyPatch(
        originalText: String,
        patchString: String
    ): String = withContext(Dispatchers.Default) {
        if (patchString.isBlank()) {
            // Empty patch means no changes — return original as-is
            return@withContext originalText
        }

        val originalLines = originalText.lines()
        val patchLines = patchString.lines()

        // Parse the unified diff back into a Patch object
        val patch: Patch<String> = UnifiedDiffUtils.parseUnifiedDiff(patchLines)

        // Apply the patch to produce the revised lines
        val revisedLines = DiffUtils.patch(originalLines, patch)

        revisedLines.joinToString("\n")
    }

    /**
     * Computes a structured line-by-line diff for display in the Diff View UI.
     *
     * Each line is tagged as EQUAL, INSERT, DELETE, or CHANGE, making it
     * straightforward to render with color-coded backgrounds.
     *
     * @param originalText The older version text.
     * @param revisedText The newer version text.
     * @return A list of [DiffLine] entries for rendering.
     */
    suspend fun computeLineDiff(
        originalText: String,
        revisedText: String
    ): List<DiffLine> = withContext(Dispatchers.Default) {
        val originalLines = originalText.lines()
        val revisedLines = revisedText.lines()

        val patch = DiffUtils.diff(originalLines, revisedLines)
        val result = mutableListOf<DiffLine>()

        // Track current position in original and revised
        var origPos = 0
        var revPos = 0

        for (delta in patch.deltas) {
            val origStart = delta.source.position
            val revStart = delta.target.position

            // Add EQUAL lines before this delta
            while (origPos < origStart) {
                result.add(
                    DiffLine(
                        type = DiffLineType.EQUAL,
                        content = originalLines[origPos],
                        oldLineNumber = origPos + 1,
                        newLineNumber = revPos + 1
                    )
                )
                origPos++
                revPos++
            }

            // Process the delta based on its type
            when (delta.type) {
                com.github.difflib.patch.DeltaType.DELETE -> {
                    // Lines removed from original
                    for (line in delta.source.lines) {
                        result.add(
                            DiffLine(
                                type = DiffLineType.DELETE,
                                content = line,
                                oldLineNumber = origPos + 1,
                                newLineNumber = null
                            )
                        )
                        origPos++
                    }
                }

                com.github.difflib.patch.DeltaType.INSERT -> {
                    // Lines added in revised
                    for (line in delta.target.lines) {
                        result.add(
                            DiffLine(
                                type = DiffLineType.INSERT,
                                content = line,
                                oldLineNumber = null,
                                newLineNumber = revPos + 1
                            )
                        )
                        revPos++
                    }
                }

                com.github.difflib.patch.DeltaType.CHANGE -> {
                    // Lines changed: show old as DELETE, new as INSERT
                    for (line in delta.source.lines) {
                        result.add(
                            DiffLine(
                                type = DiffLineType.DELETE,
                                content = line,
                                oldLineNumber = origPos + 1,
                                newLineNumber = null
                            )
                        )
                        origPos++
                    }
                    for (line in delta.target.lines) {
                        result.add(
                            DiffLine(
                                type = DiffLineType.INSERT,
                                content = line,
                                oldLineNumber = null,
                                newLineNumber = revPos + 1
                            )
                        )
                        revPos++
                    }
                }

                else -> {
                    // EQUAL — handled above, shouldn't appear here
                }
            }
        }

        // Add remaining EQUAL lines after the last delta
        while (origPos < originalLines.size && revPos < revisedLines.size) {
            result.add(
                DiffLine(
                    type = DiffLineType.EQUAL,
                    content = originalLines[origPos],
                    oldLineNumber = origPos + 1,
                    newLineNumber = revPos + 1
                )
            )
            origPos++
            revPos++
        }

        result
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diff View Data Models
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents a single line in a diff comparison.
 *
 * @property type Whether this line was equal, inserted, or deleted.
 * @property content The text content of the line.
 * @property oldLineNumber Line number in the original version (null for inserts).
 * @property newLineNumber Line number in the revised version (null for deletes).
 */
data class DiffLine(
    val type: DiffLineType,
    val content: String,
    val oldLineNumber: Int?,
    val newLineNumber: Int?
)

/**
 * Classification of a diff line for UI rendering.
 */
enum class DiffLineType {
    /** Line exists unchanged in both versions */
    EQUAL,
    /** Line was added in the new version */
    INSERT,
    /** Line was removed from the old version */
    DELETE
}
