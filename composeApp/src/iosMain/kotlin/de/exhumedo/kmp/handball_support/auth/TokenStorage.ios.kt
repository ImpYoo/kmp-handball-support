package de.exhumedo.kmp.handball_support.auth

import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

actual fun tokenStorage(): TokenStorage = NSUserDefaultsTokenStorage

private const val TOKEN_STORAGE_KEY = "handball.auth"
private val json = Json { ignoreUnknownKeys = true }

private object NSUserDefaultsTokenStorage : TokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(): StoredAuth? {
        val raw = defaults.stringForKey(TOKEN_STORAGE_KEY) ?: return null
        return runCatching { json.decodeFromString<StoredAuth>(raw) }.getOrNull()
    }

    override fun save(auth: StoredAuth) {
        defaults.setObject(json.encodeToString(StoredAuth.serializer(), auth), TOKEN_STORAGE_KEY)
    }

    override fun clear() {
        defaults.removeObjectForKey(TOKEN_STORAGE_KEY)
    }
}