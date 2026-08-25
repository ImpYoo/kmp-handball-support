package de.exhumedo.kmp.handball_support.persistence

/**
 * Persists a drawing pad stroke list. Implementations are platform-specific.
 */
interface DrawingStorage {
    fun read(): String?
    fun save(json: String)
    fun clear()
}

/**
 * Returns the platform-specific [DrawingStorage].
 *
 * Web uses `localStorage`; JVM/desktop uses a file; Android/iOS use in-memory storage.
 */
expect fun drawingStorage(): DrawingStorage

/** Storage key / file name shared by persistent implementations. */
internal const val DRAWING_STORAGE_KEY = "handball.drawing"

class InMemoryDrawingStorage : DrawingStorage {
    private var current: String? = null
    override fun read(): String? = current
    override fun save(json: String) { current = json }
    override fun clear() { current = null }
}
