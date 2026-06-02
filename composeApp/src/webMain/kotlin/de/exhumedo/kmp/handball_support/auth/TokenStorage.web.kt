package de.exhumedo.kmp.handball_support.auth

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set

actual fun tokenStorage(): TokenStorage = LocalStorageTokenStorage

private const val STORAGE_KEY = "handball.auth"
private val json = Json { ignoreUnknownKeys = true }

private object LocalStorageTokenStorage : TokenStorage {
    override fun read(): StoredAuth? {
        val raw = runCatching { localStorage[STORAGE_KEY] }.getOrNull() ?: return null
        return runCatching { json.decodeFromString<StoredAuth>(raw) }.getOrNull()
    }

    override fun save(auth: StoredAuth) {
        runCatching { localStorage[STORAGE_KEY] = json.encodeToString(StoredAuth.serializer(), auth) }
    }

    override fun clear() {
        runCatching { localStorage.removeItem(STORAGE_KEY) }
    }
}


