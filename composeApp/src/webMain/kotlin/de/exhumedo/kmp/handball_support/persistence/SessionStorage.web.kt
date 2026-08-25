package de.exhumedo.kmp.handball_support.persistence

import kotlinx.browser.localStorage

actual fun sessionStorage(): SessionStorage = LocalStorageSessionStorage

private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

private object LocalStorageSessionStorage : SessionStorage {
    override fun read(): PersistedSession? {
        val raw = runCatching { localStorage.getItem(SESSION_STORAGE_KEY) }.getOrNull() ?: return null
        return runCatching { json.decodeFromString<PersistedSession>(raw) }.getOrNull()
    }

    override fun save(session: PersistedSession) {
        runCatching { localStorage.setItem(SESSION_STORAGE_KEY, json.encodeToString(PersistedSession.serializer(), session)) }
    }

    override fun clear() {
        runCatching { localStorage.removeItem(SESSION_STORAGE_KEY) }
    }
}
