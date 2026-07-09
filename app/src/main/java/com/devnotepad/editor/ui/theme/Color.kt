package com.devnotepad.editor.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Dark Theme Palette — A rich "IDE-inspired" dark palette with vibrant accents
// ─────────────────────────────────────────────────────────────────────────────

// Primary — Electric indigo for key actions and highlights
val DarkPrimary = Color(0xFF8B5CF6)       // Vibrant purple
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF3B1F7E)
val DarkOnPrimaryContainer = Color(0xFFE0D0FF)

// Secondary — Teal for secondary actions and accents
val DarkSecondary = Color(0xFF2DD4BF)     // Bright teal
val DarkOnSecondary = Color(0xFF003731)
val DarkSecondaryContainer = Color(0xFF005048)
val DarkOnSecondaryContainer = Color(0xFFA0F0E0)

// Tertiary — Amber for warnings, highlights, annotations
val DarkTertiary = Color(0xFFFBBF24)      // Golden amber
val DarkOnTertiary = Color(0xFF3D2E00)
val DarkTertiaryContainer = Color(0xFF5C4500)
val DarkOnTertiaryContainer = Color(0xFFFFE08A)

// Background & Surface — Deep charcoal-black
val DarkBackground = Color(0xFF0F1117)
val DarkOnBackground = Color(0xFFE4E4E8)
val DarkSurface = Color(0xFF161922)
val DarkOnSurface = Color(0xFFE4E4E8)
val DarkSurfaceVariant = Color(0xFF1E2130)
val DarkOnSurfaceVariant = Color(0xFFC0C3D0)

// Error
val DarkError = Color(0xFFF87171)
val DarkOnError = Color(0xFF601414)
val DarkErrorContainer = Color(0xFF8C1D18)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

// Outline
val DarkOutline = Color(0xFF3A3D4E)
val DarkOutlineVariant = Color(0xFF2A2D3E)

// ─────────────────────────────────────────────────────────────────────────────
// Light Theme Palette — Clean, minimal with the same hue family
// ─────────────────────────────────────────────────────────────────────────────

val LightPrimary = Color(0xFF6D28D9)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFEDE5FF)
val LightOnPrimaryContainer = Color(0xFF22005D)

val LightSecondary = Color(0xFF0D9488)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFCCFBF1)
val LightOnSecondaryContainer = Color(0xFF002E27)

val LightTertiary = Color(0xFFD97706)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFEF3C7)
val LightOnTertiaryContainer = Color(0xFF3D2E00)

val LightBackground = Color(0xFFFAFAFC)
val LightOnBackground = Color(0xFF1A1C20)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1A1C20)
val LightSurfaceVariant = Color(0xFFF0F0F5)
val LightOnSurfaceVariant = Color(0xFF46464F)

val LightError = Color(0xFFDC2626)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF410002)

val LightOutline = Color(0xFFD1D5DB)
val LightOutlineVariant = Color(0xFFE5E7EB)

// ─────────────────────────────────────────────────────────────────────────────
// Syntax Highlighting Colors (used by VisualTransformation in Phase 4)
// ─────────────────────────────────────────────────────────────────────────────

object SyntaxColors {
    // Kotlin syntax
    val keyword = Color(0xFFCC78F0)       // Soft purple for keywords
    val string = Color(0xFF6FE89D)        // Green for string literals
    val comment = Color(0xFF6B7280)       // Muted gray for comments
    val annotation = Color(0xFFFBBF24)    // Amber for annotations
    val number = Color(0xFF60A5FA)        // Light blue for numeric literals
    val function = Color(0xFF38BDF8)      // Cyan for function names
    val type = Color(0xFF2DD4BF)          // Teal for types

    // Markdown syntax
    val mdHeading = Color(0xFFCC78F0)     // Purple for headings
    val mdBold = Color(0xFFE4E4E8)        // Bright white for bold
    val mdItalic = Color(0xFFA78BFA)      // Lighter purple for italic
    val mdCode = Color(0xFF6FE89D)        // Green for inline code
    val mdLink = Color(0xFF60A5FA)        // Blue for links
    val mdListMarker = Color(0xFFFBBF24)  // Amber for list bullets
}
