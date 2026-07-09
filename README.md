# DevNotepad — A Developer Text Editor with Version Control

A modern, lightweight mobile text editor for developers with an incremental delta-based version control system. Built with Kotlin, Jetpack Compose, and Room.

## Features

- **Smart Editor** — Syntax highlighting for Kotlin and Markdown
- **Delta-Based Version Control** — Incremental saves using java-diff-utils patches
- **Crash Recovery** — Auto-save to temp cache every 10 seconds
- **Undo/Redo** — In-memory edit stacks for the active session
- **Search & Replace** — Find text and replace occurrences
- **Diff View** — Line-by-line comparison of versions
- **Rollback** — Restore any previous version of a file

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Database | Room |
| Diffs | java-diff-utils |
| Concurrency | Coroutines + Flows |
| Architecture | MVVM (Single-Activity) |

## Building

Open in Android Studio and sync Gradle, or run:

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/devnotepad/editor/
├── DevNotepadApp.kt              # Application class
├── data/
│   └── local/
│       ├── DevNotepadDatabase.kt  # Room database
│       ├── dao/                   # Data Access Objects
│       └── entity/                # Room entities
├── ui/
│   ├── MainActivity.kt           # Single activity entry point
│   └── theme/                    # Material3 theme (colors, typography)
└── (phases 2-5 will add more packages)
```
