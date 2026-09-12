package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.AndroidContext
import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

actual fun pendingSyncStore(): PendingSyncStore {
    val ctx = AndroidContext.appContext
        ?: error("AndroidContext.appContext is not set.")
    return SharedPreferencesPendingSyncStore(ctx)
}

private const val KEY = "handball.pending_sync"

private class SharedPreferencesPendingSyncStore(
    context: Context,
) : PendingSyncStore {
    private val prefs = context.getSharedPreferences("handball_support_sync", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(PendingSyncEntry.serializer())

    override fun loadAll(): List<PendingSyncEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    override fun saveAll(entries: List<PendingSyncEntry>) {
        prefs.edit().putString(KEY, json.encodeToString(serializer, entries)).apply()
    }
}