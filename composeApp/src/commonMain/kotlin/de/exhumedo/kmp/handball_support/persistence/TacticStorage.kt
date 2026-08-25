package de.exhumedo.kmp.handball_support.persistence

/**
 * Persists tactic board token positions. Implementations are platform-specific.
 */
interface TacticStorage {
    fun read(): String?
    fun save(json: String)
    fun clear()
}

/**
 * Returns the platform-specific [TacticStorage].
 *
 * Web uses `localStorage`; JVM/desktop uses a file; Android/iOS use in-memory storage.
 */
expect fun tacticStorage(): TacticStorage

/** Storage key / file name shared by persistent implementations. */
internal const val TACTIC_STORAGE_KEY = "handball.tactic"

class InMemoryTacticStorage : TacticStorage {
    private var current: String? = null
    override fun read(): String? = current
    override fun save(json: String) { current = json }
    override fun clear() { current = null }
}
