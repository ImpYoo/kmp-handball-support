package de.exhumedo.kmp.handball_support.persistence

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set

actual fun sessionStorage(): SessionStorage = LocalStorageSessionStorage

private val json = Json { ignoreUnknownKeys = true }

private object LocalStorageSessionStorage : SessionStorage {
    override fun read(): PersistedSession? {
        val raw = runCatching { localStorage[SESSION_STORAGE_KEY] }.getOrNull() ?: return null
        return runCatching { json.decodeFromString<PersistedSession>(raw) }.getOrNull()
    }

    override fun save(session: PersistedSession) {
        runCatching { localStorage[SESSION_STORAGE_KEY] = json.encodeToString(PersistedSession.serializer(), session) }
    }

    override fun clear() {
        runCatching { localStorage.removeItem(SESSION_STORAGE_KEY) }
    }
}

