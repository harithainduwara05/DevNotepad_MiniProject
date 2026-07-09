package com.devnotepad.editor.ui.navigation

/**
 * Defines all navigation routes for the single-activity Compose navigation.
 *
 * Each route is a string constant used with NavHost and NavController.
 * Parameterized routes use curly-brace placeholders (e.g., {docId}).
 */
object NavRoutes {

    /** Main editor screen — the default/start destination */
    const val EDITOR = "editor"

    /** Version history list for the current document */
    const val VERSION_HISTORY = "version_history/{docId}"

    /** Diff view comparing two versions */
    const val DIFF_VIEW = "diff_view/{docId}/{fromVersion}/{toVersion}"

    /** Markdown preview screen */
    const val MARKDOWN_PREVIEW = "markdown_preview"

    // ─────────────────────────────────────────────────────────────────
    // Route builder helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Builds the version history route for a specific document.
     *
     * @param docId The Room database ID of the document.
     */
    fun versionHistory(docId: Long): String {
        return "version_history/$docId"
    }

    /**
     * Builds the diff view route for comparing two versions.
     *
     * @param docId The Room database ID of the document.
     * @param fromVersion The older version number.
     * @param toVersion The newer version number.
     */
    fun diffView(docId: Long, fromVersion: Int, toVersion: Int): String {
        return "diff_view/$docId/$fromVersion/$toVersion"
    }
}
