# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities and DAOs
-keep class com.devnotepad.editor.data.local.entity.** { *; }
-keep class com.devnotepad.editor.data.local.dao.** { *; }

# Keep java-diff-utils classes
-keep class com.github.difflib.** { *; }

# General rules for Compose
-dontwarn androidx.compose.**
