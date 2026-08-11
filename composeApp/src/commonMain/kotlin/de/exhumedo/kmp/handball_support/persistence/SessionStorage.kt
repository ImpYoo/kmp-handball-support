package de.exhumedo.kmp.handball_support.persistence

/**
 * Persists a single [PersistedSession]. Implementations are platform-specific.
 */
interface SessionStorage {
    fun read(): PersistedSession?
    fun save(session: PersistedSession)
    fun clear()
}

/**
 * Returns the platform-specific [SessionStorage].
 *
 * Web is backed by `localStorage`, JVM/desktop by a file in the user home;
 * Android and iOS currently use [InMemorySessionStorage].
 */
expect fun sessionStorage(): SessionStorage

/** Storage key / file name shared by the persistent implementations. */
internal const val SESSION_STORAGE_KEY = "handball.session"

class InMemorySessionStorage : SessionStorage {
    private var current: PersistedSession? = null
    override fun read(): PersistedSession? = current
    override fun save(session: PersistedSession) { current = session }
    override fun clear() { current = null }
}

