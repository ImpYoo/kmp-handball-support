package de.exhumedo.kmp.handball_support.persistence

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

actual fun pendingSyncStore(): PendingSyncStore = NSUserDefaultsPendingSyncStore

private const val KEY = "handball.pending_sync"
private val json = Json { ignoreUnknownKeys = true }
private val serializer = ListSerializer(PendingSyncEntry.serializer())

private object NSUserDefaultsPendingSyncStore : PendingSyncStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun loadAll(): List<PendingSyncEntry> {
        val raw = defaults.stringForKey(KEY) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    override fun saveAll(entries: List<PendingSyncEntry>) {
        if (entries.isEmpty()) {
            defaults.removeObjectForKey(KEY)
        } else {
            defaults.setObject(json.encodeToString(serializer, entries), KEY)
        }
    }
}