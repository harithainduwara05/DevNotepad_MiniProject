package com.devnotepad.editor.data.model

/**
 * Utility for detecting the syntax highlighting mode from a file path.
 *
 * Examines the file extension and maps it to the appropriate [SyntaxMode].
 * Falls back to [SyntaxMode.NONE] for unrecognized extensions.
 */
object SyntaxDetector {

    /**
     * Pre-built lookup table mapping file extensions to syntax modes.
     * Extensions are stored lowercase without the leading dot.
     */
    private val extensionMap: Map<String, SyntaxMode> by lazy {
        SyntaxMode.entries.flatMap { mode ->
            mode.extensions.map { ext -> ext.lowercase() to mode }
        }.toMap()
    }

    /**
     * Detects the syntax mode for a given file path.
     *
     * @param filePath The absolute or relative file path.
     * @return The detected [SyntaxMode], or [SyntaxMode.NONE] if unknown.
     */
    fun detectFromPath(filePath: String): SyntaxMode {
        val extension = filePath
            .substringAfterLast('.', "")
            .lowercase()
            .trim()

        if (extension.isEmpty()) return SyntaxMode.NONE

        return extensionMap[extension] ?: SyntaxMode.NONE
    }

    /**
     * Detects the syntax mode from a file name.
     * Convenience overload that delegates to [detectFromPath].
     */
    fun detectFromName(fileName: String): SyntaxMode {
        return detectFromPath(fileName)
    }
}
