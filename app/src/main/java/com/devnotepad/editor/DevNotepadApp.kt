package com.devnotepad.editor

import android.app.Application
import com.devnotepad.editor.data.local.DevNotepadDatabase

/**
 * Custom Application class for DevNotepad.
 *
 * Serves as the single source of truth for app-wide singletons such as
 * the Room database instance. Using lazy initialization ensures the
 * database is only created when first accessed, not at app startup.
 */
class DevNotepadApp : Application() {

    /**
     * Lazily-initialized Room database instance.
     * Shared across the app via (application as DevNotepadApp).database
     */
    val database: DevNotepadDatabase by lazy {
        DevNotepadDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        /**
         * Static reference to the Application instance.
         * Safe to use because Application outlives all components.
         */
        lateinit var instance: DevNotepadApp
            private set
    }
}
