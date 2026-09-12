package de.exhumedo.kmp.handball_support.persistence

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

actual fun pendingSyncStore(): PendingSyncStore = FilePendingSyncStore()

private const val FILE_NAME = "pending-sync.json"

private class FilePendingSyncStore : PendingSyncStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val serializer = ListSerializer(PendingSyncEntry.serializer())
    private val file: File =
        File(System.getProperty("user.home"), ".handball-support").resolve(FILE_NAME)

    override fun loadAll(): List<PendingSyncEntry> {
        if (!file.exists()) return emptyList()
        return runCatching { json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
    }

    override fun saveAll(entries: List<PendingSyncEntry>) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(serializer, entries))
        }
    }
}