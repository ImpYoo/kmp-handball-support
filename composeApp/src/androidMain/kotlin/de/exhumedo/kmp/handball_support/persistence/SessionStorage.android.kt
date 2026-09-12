package de.exhumedo.kmp.handball_support.persistence

import android.content.Context
import de.exhumedo.kmp.handball_support.AndroidContext
import kotlinx.serialization.json.Json

actual fun sessionStorage(): SessionStorage {
    val ctx = AndroidContext.appContext
        ?: error("AndroidContext.appContext is not set. Call AndroidContext.appContext = applicationContext in MainActivity.onCreate.")
    return SharedPreferencesSessionStorage(ctx)
}

private class SharedPreferencesSessionStorage(
    context: Context,
) : SessionStorage {
    private val prefs = context.getSharedPreferences("handball_support", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun read(): PersistedSession? {
        val raw = prefs.getString(SESSION_STORAGE_KEY, null) ?: return null
        return runCatching { json.decodeFromString<PersistedSession>(raw) }.getOrNull()
    }

    override fun save(session: PersistedSession) {
        prefs.edit().putString(SESSION_STORAGE_KEY, json.encodeToString(PersistedSession.serializer(), session)).apply()
    }

    override fun clear() {
        prefs.edit().remove(SESSION_STORAGE_KEY).apply()
    }
}