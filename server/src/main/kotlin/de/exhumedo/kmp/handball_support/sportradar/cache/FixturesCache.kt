package de.exhumedo.kmp.handball_support.sportradar.cache

import de.exhumedo.kmp.handball_support.sportradar.model.Phase
import java.util.concurrent.ConcurrentHashMap

// ── Optional fixtures cache ───────────────────────────────────────────────────
// This is a standalone module. To DISABLE caching, pass null for the cache
// parameter in SportradarPhaseRepository — no other code changes needed:
//
//   SportradarPhaseRepository(client, configs, cache = null)   // caching OFF
//   SportradarPhaseRepository(client, configs, cache = FixturesCache()) // ON
//
// Uses ConcurrentHashMap for thread-safe reads/writes on JVM.
// For strict KMP commonMain, inject a platform-specific thread-safe map adapter.
// ─────────────────────────────────────────────────────────────────────────────

class FixturesCache(
    val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    /** Inject for deterministic tests; defaults to wall-clock time on JVM */
    private val clock: () -> Long = { System.currentTimeMillis() },
    // KMP migration: replace with { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() }
) {
    private data class Entry(val phases: List<Phase>, val expiresAt: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    /** Returns cached phases if the entry exists and has not expired. */
    fun get(key: String): List<Phase>? {
        val entry = entries[key] ?: return null
        return if (clock() < entry.expiresAt) {
            entry.phases
        } else {
            entries.remove(key)
            null
        }
    }

    /** Stores phases under [key] with a TTL relative to now. */
    fun put(key: String, phases: List<Phase>) {
        entries[key] = Entry(phases = phases, expiresAt = clock() + ttlMillis)
    }

    /** Evicts all cached entries — useful when you know upstream data has changed. */
    fun invalidateAll() {
        entries.clear()
    }

    companion object {
        /** Default: 5 minutes */
        const val DEFAULT_TTL_MILLIS: Long = 5 * 60 * 1_000L
    }
}

