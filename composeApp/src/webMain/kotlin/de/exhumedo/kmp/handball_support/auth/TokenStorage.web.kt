package de.exhumedo.kmp.handball_support.auth

import kotlinx.browser.window
import kotlinx.serialization.json.Json

actual fun tokenStorage(): TokenStorage = LocalStorageTokenStorage

private const val STORAGE_KEY = "handball.auth"
private val json = Json { ignoreUnknownKeys = true }

private object LocalStorageTokenStorage : TokenStorage {
    override fun read(): StoredAuth? {
        val raw = runCatching { window.localStorage.getItem(STORAGE_KEY) }.getOrNull() ?: return null
        return runCatching { json.decodeFromString<StoredAuth>(raw) }.getOrNull()
    }

    override fun save(auth: StoredAuth) {
        runCatching { window.localStorage.setItem(STORAGE_KEY, json.encodeToString(StoredAuth.serializer(), auth)) }
    }

    override fun clear() {
        runCatching { window.localStorage.removeItem(STORAGE_KEY) }
    }
}


