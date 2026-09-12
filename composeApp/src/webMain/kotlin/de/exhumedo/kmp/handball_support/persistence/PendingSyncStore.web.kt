package de.exhumedo.kmp.handball_support.persistence

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.browser.window

actual fun pendingSyncStore(): PendingSyncStore = LocalStoragePendingSyncStore

private const val KEY = "handball.pending_sync"
private val json = Json { ignoreUnknownKeys = true }
private val serializer = ListSerializer(PendingSyncEntry.serializer())

private object LocalStoragePendingSyncStore : PendingSyncStore {
    override fun loadAll(): List<PendingSyncEntry> {
        val raw = runCatching { window.localStorage.getItem(KEY) }.getOrNull() ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    override fun saveAll(entries: List<PendingSyncEntry>) {
        runCatching {
            if (entries.isEmpty()) {
                window.localStorage.removeItem(KEY)
            } else {
                window.localStorage.setItem(KEY, json.encodeToString(serializer, entries))
            }
        }
    }
}