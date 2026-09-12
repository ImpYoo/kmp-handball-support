package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.client.CreateCoachingEvaluationRequestDto
import kotlinx.serialization.Serializable

/**
 * A coaching evaluation payload waiting to be synced to the server.
 *
 * Stored locally when the device is offline or no token is available.
 * Drained to the server when connectivity and authentication are restored.
 */
@Serializable
data class PendingSyncEntry(
    /** Local UUID; replaced by the server-assigned id on successful sync. */
    val localId: String,
    /** Server-side evaluation id if a previous sync succeeded and we're updating. */
    val evaluationId: String? = null,
    /** The payload to send to the server. */
    val payload: CreateCoachingEvaluationRequestDto,
    /** ISO-8601 timestamp when the entry was first queued. */
    val queuedAt: String,
    /** Number of failed sync attempts (for diagnostics / backoff). */
    val attemptCount: Int = 0,
)

/**
 * Persistent queue of coaching evaluations awaiting server sync.
 *
 * Implementations are platform-specific: Android uses SharedPreferences,
 * iOS uses NSUserDefaults, Web uses localStorage, JVM uses a file.
 */
interface PendingSyncStore {
    /** Returns all pending entries, oldest first. */
    fun loadAll(): List<PendingSyncEntry>

    /** Replaces the entire queue. */
    fun saveAll(entries: List<PendingSyncEntry>)

    /** Adds an entry to the end of the queue. */
    fun enqueue(entry: PendingSyncEntry) {
        saveAll(loadAll() + entry)
    }

    /** Removes an entry by its local id. */
    fun remove(localId: String) {
        saveAll(loadAll().filterNot { it.localId == localId })
    }

    /** Returns the number of pending entries. */
    fun pendingCount(): Int = loadAll().size

    /** Removes all entries. */
    fun clear() {
        saveAll(emptyList())
    }
}

/**
 * Returns the platform-specific [PendingSyncStore].
 */
expect fun pendingSyncStore(): PendingSyncStore

/**
 * In-memory fallback used when no platform store is wired.
 */
class InMemoryPendingSyncStore : PendingSyncStore {
    private val entries = mutableListOf<PendingSyncEntry>()
    override fun loadAll(): List<PendingSyncEntry> = entries.toList()
    override fun saveAll(entries: List<PendingSyncEntry>) {
        this.entries.clear()
        this.entries.addAll(entries)
    }
}