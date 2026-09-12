package de.exhumedo.kmp.handball_support

import android.content.Context

/**
 * Global reference to the Android application context, set from [MainActivity.onCreate].
 * Used by platform storage implementations (SharedPreferences) that need a [Context]
 * but are constructed via `expect fun` factories without parameters.
 */
object AndroidContext {
    @Volatile
    var appContext: Context? = null
}