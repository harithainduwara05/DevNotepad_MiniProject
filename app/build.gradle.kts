plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.devnotepad.editor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devnotepad.editor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export directory for migration support
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ───────────────────────────────────────────────────────────────
    // Core Android & Lifecycle
    // ───────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // ───────────────────────────────────────────────────────────────
    // Jetpack Compose (BOM ensures compatible versions across all
    // Compose artifacts without specifying individual versions)
    // ───────────────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // ───────────────────────────────────────────────────────────────
    // Room Persistence Library
    // Stores version metadata: Document entries and VersionSnapshot
    // entities for delta-based version control.
    // ───────────────────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // KSP annotation processor for Room

    // ───────────────────────────────────────────────────────────────
    // Kotlin Coroutines
    // Used for background auto-save, file I/O, and patch computation
    // ───────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // ───────────────────────────────────────────────────────────────
    // java-diff-utils
    // Core library for computing unified diffs and applying patches.
    // Powers the incremental (delta-based) version control system.
    // ───────────────────────────────────────────────────────────────
    implementation(libs.java.diff.utils)

    // ───────────────────────────────────────────────────────────────
    // DataStore Preferences
    // Lightweight key-value storage for user preferences such as
    // word-wrap toggle, theme selection, and recent file list.
    // ───────────────────────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ───────────────────────────────────────────────────────────────
    // Testing
    // ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
