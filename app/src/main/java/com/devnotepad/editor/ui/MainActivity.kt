package com.devnotepad.editor.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devnotepad.editor.ui.navigation.NavRoutes
import com.devnotepad.editor.ui.screens.DiffViewScreen
import com.devnotepad.editor.ui.screens.EditorScreen
import com.devnotepad.editor.ui.screens.MarkdownPreviewScreen
import com.devnotepad.editor.ui.screens.VersionHistoryScreen
import com.devnotepad.editor.ui.theme.DevNotepadTheme
import com.devnotepad.editor.ui.viewmodel.EditorViewModel
import com.devnotepad.editor.ui.viewmodel.EditorViewModelFactory

/**
 * Single Activity for the entire DevNotepad app.
 *
 * All UI is rendered via Jetpack Compose. Navigation between screens
 * (Editor, Version History, Diff View, Markdown Preview) is handled
 * by Navigation Compose within a single NavHost.
 *
 * The [EditorViewModel] is scoped to this Activity so it survives
 * navigation between screens (configuration changes are handled by
 * the ViewModel lifecycle).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge rendering (content draws behind system bars)
        enableEdgeToEdge()

        setContent {
            DevNotepadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DevNotepadNavigation()
                }
            }
        }
    }
}

/**
 * Root navigation composable for the app.
 *
 * Defines the navigation graph with the following routes:
 *  - [NavRoutes.EDITOR] — Main editor screen (start destination)
 *  - [NavRoutes.VERSION_HISTORY] — Version history timeline
 *  - [NavRoutes.DIFF_VIEW] — Line-by-line diff comparison
 *  - [NavRoutes.MARKDOWN_PREVIEW] — Markdown rendered preview
 *
 * The [EditorViewModel] is shared across all screens via the Activity
 * scope, ensuring state consistency when navigating.
 */
@Composable
fun DevNotepadNavigation() {
    val navController = rememberNavController()

    // Shared ViewModel scoped to the Activity (survives nav changes)
    val editorViewModel: EditorViewModel = viewModel(
        factory = EditorViewModelFactory()
    )

    NavHost(
        navController = navController,
        startDestination = NavRoutes.EDITOR
    ) {
        // ── Main Editor Screen ──
        composable(NavRoutes.EDITOR) {
            EditorScreen(
                viewModel = editorViewModel,
                onNavigateToVersionHistory = { docId ->
                    navController.navigate(NavRoutes.versionHistory(docId))
                },
                onNavigateToMarkdownPreview = {
                    navController.navigate(NavRoutes.MARKDOWN_PREVIEW)
                }
            )
        }

        // ── Version History Screen ──
        composable(
            route = NavRoutes.VERSION_HISTORY,
            arguments = listOf(
                navArgument("docId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: return@composable

            VersionHistoryScreen(
                documentId = docId,
                editorViewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiff = { fromVersion, toVersion ->
                    navController.navigate(NavRoutes.diffView(docId, fromVersion, toVersion))
                }
            )
        }

        // ── Diff View Screen ──
        composable(
            route = NavRoutes.DIFF_VIEW,
            arguments = listOf(
                navArgument("docId") { type = NavType.LongType },
                navArgument("fromVersion") { type = NavType.IntType },
                navArgument("toVersion") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: return@composable
            val fromVersion = backStackEntry.arguments?.getInt("fromVersion") ?: return@composable
            val toVersion = backStackEntry.arguments?.getInt("toVersion") ?: return@composable

            DiffViewScreen(
                documentId = docId,
                fromVersion = fromVersion,
                toVersion = toVersion,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Markdown Preview Screen ──
        composable(NavRoutes.MARKDOWN_PREVIEW) {
            val editorState by editorViewModel.editorState.collectAsState()

            MarkdownPreviewScreen(
                markdownText = editorState.content,
                fileName = editorState.fileName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
