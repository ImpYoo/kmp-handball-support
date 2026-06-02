package de.exhumedo.kmp.handball_support.auth

interface TokenStorage {
    fun read(): StoredAuth?
    fun save(auth: StoredAuth)
    fun clear()
}

/**
 * Returns the platform-specific [TokenStorage] implementation.
 *
 * Web is backed by `localStorage`; Android, iOS and JVM currently use [InMemoryTokenStorage].
 * Replace the latter with platform-secure storage (Keystore / Keychain / OS keyring) before
 * shipping mobile or desktop builds to production.
 */
expect fun tokenStorage(): TokenStorage

class InMemoryTokenStorage : TokenStorage {
    private var current: StoredAuth? = null
    override fun read(): StoredAuth? = current
    override fun save(auth: StoredAuth) { current = auth }
    override fun clear() { current = null }
}


