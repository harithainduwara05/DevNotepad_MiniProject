# 📝 DevNotepad — Mobile Text Editor with Version Control

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose)

**DevNotepad** is a modern, lightweight, and highly performant Android text editor built specifically for developers. Developed as a Computer Science mini-project, it features a unique **Incremental Delta-Based Version Control System**, allowing users to seamlessly track changes, compare versions, and safely edit code on the go without consuming excessive local storage.

---

## ✨ Key Features

- **🧠 Smart Syntax Highlighting** — Dynamic, regex-based real-time syntax highlighting for Kotlin (`.kt`, `.kts`) and Markdown (`.md`) files.
- **👀 Markdown Preview** — Dedicated read-only preview screen that beautifully renders Markdown elements (headings, bold, lists, code blocks, etc.) into styled text.
- **🔄 Incremental Version Control** — Saves storage space by storing only the *differences* (deltas) between file saves using `java-diff-utils`, rather than duplicating entire files for every version.
- **🛡️ Auto-Save & Crash Recovery** — Background coroutines automatically back up your unsaved changes to a temporary cache every 10 seconds. Never lose your progress!
- **🔍 Diff View (Version Comparison)** — Visually compare any two versions of a file with line-by-line additions and deletions highlighted (green for additions, red for deletions).
- **⏪ Rollback Mechanism** — Easily restore any document to a previous state, safely rewinding your document history.
- **🔁 In-Memory Undo/Redo** — Fully functional Undo/Redo stack for active editing sessions.
- **🔎 Find & Replace** — Integrated, blazing-fast search bar to find and replace text across large files.

---

## 🛠️ Technology Stack

DevNotepad is built with modern Android development practices and libraries:

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin (JDK 17) |
| **UI Toolkit** | Jetpack Compose (Material 3 Design Guidelines) |
| **Database** | Room Persistence Library (SQLite) |
| **Diff Engine** | `java-diff-utils` |
| **Concurrency** | Kotlin Coroutines & Flows |
| **Architecture** | MVVM (Single-Activity Architecture) |
| **Local Storage** | DataStore Preferences (for UI toggles & settings) |

---

## 🏗️ Core Architecture & Mechanics

### 1. Storage & Delta Tracking Mechanism
DevNotepad employs an efficient versioning system to track file history without bloating storage:
- **Base Version:** The very first save of a file is stored completely.
- **Deltas (Patches):** Subsequent saves only compute and store the Unified Diff (patch) against the previous version.
- **Reconstruction:** When previewing or restoring an older version, the app starts from the Base Version and applies patches sequentially up to the requested version in memory.
- **Database:** All metadata (timestamps, file paths, versions, relationships) is securely managed via **Room**.

### 2. File Safety & Crash Prevention
A built-in 10-second interval worker checks the editor buffer for unsaved changes. If the app crashes or is aggressively killed by the Android OS to reclaim memory, a **Recovery Dialog** greets the user upon reopening, allowing them to restore their lost work instantly.

### 3. Syntax Highlighting Engine
Built entirely in Jetpack Compose using `VisualTransformation`, the editor styles text on the fly without modifying the underlying text data. It uses a priority-based tokenizer to ensure elements like strings and comments are styled accurately without conflicting with language keywords.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Giraffe or newer recommended)
- **JDK 17**
- An Android Emulator or physical device running **Android 8.0 (API 26)** or higher.

### Installation & Build
1. Clone the repository to your local machine:
   ```bash
   git clone https://github.com/harithainduwara05/DevNotepad_MiniProject.git
   ```
2. Open the project folder in **Android Studio**.
3. Allow Gradle to sync all dependencies automatically.
4. Run the app on your connected device/emulator, or build the APK via the command line:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📂 Project Structure Overview

```text
app/src/main/java/com/devnotepad/editor/
├── DevNotepadApp.kt              # Application class (Main entry point)
├── data/                         # Data layer
│   ├── local/                    # Room Database, DAOs, Entities
│   ├── model/                    # Data classes & Enums (SyntaxMode, etc.)
│   └── repository/               # File & Database Repositories
├── ui/                           # Presentation layer (Jetpack Compose)
│   ├── MainActivity.kt           # Single-Activity entry point
│   ├── components/               # Reusable Compose Widgets (Toolbar, TextField)
│   ├── navigation/               # NavRoutes for Compose Navigation
│   ├── screens/                  # App Screens (Editor, DiffView, History, Preview)
│   ├── theme/                    # Material 3 Theme, Typography, Syntax Colors
│   └── viewmodel/                # ViewModels (EditorViewModel, HistoryViewModel)
└── highlight/                    # Syntax Highlighting & Tokenization Logic
```

---

## 📸 Screenshots

*(Add your application screenshots here to showcase the beautiful UI!)*

| Editor | Version History | Diff View | Markdown Preview |
|:---:|:---:|:---:|:---:|
| <img src="" width="200" alt="Editor Screen"> | <img src="" width="200" alt="History Screen"> | <img src="" width="200" alt="Diff Screen"> | <img src="" width="200" alt="Preview Screen"> |

---
