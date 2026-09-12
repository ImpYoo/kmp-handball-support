package de.exhumedo.kmp.handball_support.auth

import android.content.Context
import de.exhumedo.kmp.handball_support.AndroidContext
import kotlinx.serialization.json.Json

actual fun tokenStorage(): TokenStorage {
    val ctx = AndroidContext.appContext
        ?: error("AndroidContext.appContext is not set. Call AndroidContext.appContext = applicationContext in MainActivity.onCreate.")
    return SharedPreferencesTokenStorage(ctx)
}

private const val TOKEN_STORAGE_KEY = "handball.auth"

private class SharedPreferencesTokenStorage(
    context: Context,
) : TokenStorage {
    private val prefs = context.getSharedPreferences("handball_support_auth", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun read(): StoredAuth? {
        val raw = prefs.getString(TOKEN_STORAGE_KEY, null) ?: return null
        return runCatching { json.decodeFromString<StoredAuth>(raw) }.getOrNull()
    }

    override fun save(auth: StoredAuth) {
        prefs.edit().putString(TOKEN_STORAGE_KEY, json.encodeToString(StoredAuth.serializer(), auth)).apply()
    }

    override fun clear() {
        prefs.edit().remove(TOKEN_STORAGE_KEY).apply()
    }
}