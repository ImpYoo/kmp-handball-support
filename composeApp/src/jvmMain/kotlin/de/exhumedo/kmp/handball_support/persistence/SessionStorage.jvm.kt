package de.exhumedo.kmp.handball_support.persistence

import kotlinx.serialization.json.Json
import java.io.File

actual fun sessionStorage(): SessionStorage = FileSessionStorage()

private class FileSessionStorage : SessionStorage {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File =
        File(System.getProperty("user.home"), ".handball-support").resolve("$SESSION_STORAGE_KEY.json")

    override fun read(): PersistedSession? {
        if (!file.exists()) return null
        return runCatching { json.decodeFromString<PersistedSession>(file.readText()) }.getOrNull()
    }

    override fun save(session: PersistedSession) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(PersistedSession.serializer(), session))
        }
    }

    override fun clear() {
        runCatching { if (file.exists()) file.delete() }
    }
}

