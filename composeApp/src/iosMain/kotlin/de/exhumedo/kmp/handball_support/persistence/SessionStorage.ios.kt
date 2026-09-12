package de.exhumedo.kmp.handball_support.persistence

import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

actual fun sessionStorage(): SessionStorage = NSUserDefaultsSessionStorage

private val json = Json { ignoreUnknownKeys = true }

private object NSUserDefaultsSessionStorage : SessionStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(): PersistedSession? {
        val raw = defaults.stringForKey(SESSION_STORAGE_KEY) ?: return null
        return runCatching { json.decodeFromString<PersistedSession>(raw) }.getOrNull()
    }

    override fun save(session: PersistedSession) {
        defaults.setObject(json.encodeToString(PersistedSession.serializer(), session), SESSION_STORAGE_KEY)
    }

    override fun clear() {
        defaults.removeObjectForKey(SESSION_STORAGE_KEY)
    }
}