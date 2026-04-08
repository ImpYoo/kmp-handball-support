package de.exhumedo.kmp.handball_support.persistence.auth

import de.exhumedo.kmp.handball_support.auth.AuthBootstrapException
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUser
import de.exhumedo.kmp.handball_support.auth.AuthUserAlreadyExistsException
import de.exhumedo.kmp.handball_support.auth.AuthUserNotFoundException
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.auth.BootstrapAdmin
import de.exhumedo.kmp.handball_support.auth.LastEnabledAdminRemovalException
import de.exhumedo.kmp.handball_support.security.PasswordHasher
import de.exhumedo.kmp.handball_support.security.Pbkdf2PasswordHasher
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * File-backed auth user store with hashed passwords and JSON persistence.
 *
 * @property storagePath JSON file containing persisted auth users.
 * @property passwordHasher Password hasher used for persistence and verification.
 * @property clock Clock used to stamp created and updated timestamps.
 * @property bootstrapAdmin Optional bootstrap admin used when the storage file is empty.
 */
class JsonFileAuthUserStore(
    private val storagePath: Path,
    private val passwordHasher: PasswordHasher = Pbkdf2PasswordHasher(),
    private val clock: Clock = Clock.System,
    private val bootstrapAdmin: BootstrapAdmin? = null,
) : AuthUserStore {
    private val lock = Any()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        ensureStorageExists()
    }

    /**
     * Authenticates a user against the persisted JSON store.
     */
    override fun authenticate(username: String, password: String): AuthUser? = synchronized(lock) {
        val normalizedUsername = normalizeUsername(username)
        val user = loadAll().firstOrNull { it.username == normalizedUsername && it.enabled } ?: return null
        if (!passwordHasher.verify(password, user.passwordHash)) {
            return null
        }

        user
    }

    /**
     * Returns all users sorted by username.
     */
    override fun findAll(): List<AuthUser> = synchronized(lock) {
        loadAll().sortedBy { it.username }
    }

    /**
     * Finds a single user by username.
     */
    override fun findByUsername(username: String): AuthUser? = synchronized(lock) {
        val normalizedUsername = normalizeUsername(username)
        loadAll().firstOrNull { it.username == normalizedUsername }
    }

    /**
     * Creates a new user and persists the hashed password.
     */
    override fun createUser(
        username: String,
        password: String,
        role: AuthRole,
        enabled: Boolean,
    ): AuthUser = synchronized(lock) {
        val normalizedUsername = normalizeUsername(username)
        validateUsername(normalizedUsername)
        validatePassword(password)

        val users = loadAll().toMutableList()
        if (users.any { it.username == normalizedUsername }) {
            throw AuthUserAlreadyExistsException(normalizedUsername)
        }

        val timestamp = clock.now().toString()
        val user = AuthUser(
            username = normalizedUsername,
            passwordHash = passwordHasher.hash(password),
            role = role,
            enabled = enabled,
            createdAt = timestamp,
            updatedAt = timestamp,
            tokenInvalidBefore = null,
        )

        users.add(user)
        persistAll(users)
        user
    }

    /**
     * Updates an existing user and persists the modified state.
     */
    override fun updateUser(
        username: String,
        password: String?,
        role: AuthRole?,
        enabled: Boolean?,
    ): AuthUser = synchronized(lock) {
        if (password == null && role == null && enabled == null) {
            throw IllegalArgumentException("At least one auth user field must be provided for update.")
        }

        val normalizedUsername = normalizeUsername(username)
        val users = loadAll().toMutableList()
        val index = users.indexOfFirst { it.username == normalizedUsername }
        if (index < 0) {
            throw AuthUserNotFoundException(normalizedUsername)
        }

        val existing = users[index]
        password?.let { validatePassword(it) }

        val updated = existing.copy(
            passwordHash = password?.let(passwordHasher::hash) ?: existing.passwordHash,
            role = role ?: existing.role,
            enabled = enabled ?: existing.enabled,
            updatedAt = clock.now().toString(),
        )

        ensureAdminSafety(users, existing, updated, deleteRequested = false)

        users[index] = updated
        persistAll(users)
        updated
    }

    /**
     * Persists a new token invalid-before marker for the specified user.
     */
    override fun revokeTokens(
        username: String,
        invalidBefore: String,
    ): AuthUser = synchronized(lock) {
        require(invalidBefore.isNotBlank()) { "Token invalid-before timestamp must not be blank." }

        val normalizedUsername = normalizeUsername(username)
        val users = loadAll().toMutableList()
        val index = users.indexOfFirst { it.username == normalizedUsername }
        if (index < 0) {
            throw AuthUserNotFoundException(normalizedUsername)
        }

        val existing = users[index]
        val updated = existing.copy(
            updatedAt = clock.now().toString(),
            tokenInvalidBefore = invalidBefore,
        )

        users[index] = updated
        persistAll(users)
        updated
    }

    /**
     * Deletes a user from persistent storage.
     */
    override fun deleteUser(username: String) = synchronized(lock) {
        val normalizedUsername = normalizeUsername(username)
        val users = loadAll().toMutableList()
        val existing = users.firstOrNull { it.username == normalizedUsername }
            ?: throw AuthUserNotFoundException(normalizedUsername)

        ensureAdminSafety(users, existing, replacement = null, deleteRequested = true)
        users.removeIf { it.username == normalizedUsername }
        persistAll(users)
    }

    private fun ensureStorageExists() {
        storagePath.parent?.let { Files.createDirectories(it) }
        if (!Files.exists(storagePath)) {
            Files.writeString(storagePath, "[]")
        }

        synchronized(lock) {
            val users = loadAll()
            if (users.isEmpty()) {
                val admin = bootstrapAdmin
                    ?: throw AuthBootstrapException(
                        "Auth user storage '${storagePath}' is empty and no bootstrap admin password was configured.",
                    )

                validateUsername(normalizeUsername(admin.username))
                validatePassword(admin.password)

                val timestamp = clock.now().toString()
                persistAll(
                    listOf(
                        AuthUser(
                            username = normalizeUsername(admin.username),
                            passwordHash = passwordHasher.hash(admin.password),
                            role = AuthRole.ADMIN,
                            enabled = true,
                            createdAt = timestamp,
                            updatedAt = timestamp,
                            tokenInvalidBefore = null,
                        ),
                    ),
                )
            }
        }
    }

    private fun ensureAdminSafety(
        existingUsers: List<AuthUser>,
        current: AuthUser,
        replacement: AuthUser?,
        deleteRequested: Boolean,
    ) {
        val currentIsEnabledAdmin = current.role == AuthRole.ADMIN && current.enabled
        if (!currentIsEnabledAdmin) {
            return
        }

        val otherEnabledAdmins = existingUsers.count {
            it.username != current.username && it.role == AuthRole.ADMIN && it.enabled
        }
        val replacementStillEnabledAdmin = replacement?.role == AuthRole.ADMIN && replacement.enabled

        if (deleteRequested || !replacementStillEnabledAdmin) {
            if (otherEnabledAdmins == 0) {
                throw LastEnabledAdminRemovalException(current.username)
            }
        }
    }

    private fun loadAll(): List<AuthUser> {
        val content = Files.readString(storagePath)
        if (content.isBlank()) {
            return emptyList()
        }

        return json.decodeFromString<List<StoredAuthUser>>(content).map { it.toDomain() }
    }

    private fun persistAll(users: List<AuthUser>) {
        Files.writeString(storagePath, json.encodeToString(users.map { it.toStored() }))
    }

    private fun normalizeUsername(username: String): String = username.trim().lowercase()

    private fun validateUsername(username: String) {
        require(username.matches(USERNAME_PATTERN)) {
            "Username '$username' must be 3-50 characters and contain only letters, digits, '.', '_' or '-'."
        }
    }

    private fun validatePassword(password: String) {
        require(password.length >= 12) { "Password must be at least 12 characters long." }
        require(password.any(Char::isUpperCase)) { "Password must contain at least one uppercase letter." }
        require(password.any(Char::isLowerCase)) { "Password must contain at least one lowercase letter." }
        require(password.any(Char::isDigit)) { "Password must contain at least one digit." }
        require(password.any { !it.isLetterOrDigit() && !it.isWhitespace() }) {
            "Password must contain at least one special character."
        }
        require(password.none(Char::isWhitespace)) { "Password must not contain whitespace." }
    }

    private companion object {
        val USERNAME_PATTERN = Regex("^[a-z0-9._-]{3,50}$")
    }
}

@Serializable
private data class StoredAuthUser(
    val username: String,
    val passwordHash: String,
    val role: String,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val tokenInvalidBefore: String? = null,
)

private fun StoredAuthUser.toDomain(): AuthUser = AuthUser(
    username = username,
    passwordHash = passwordHash,
    role = AuthRole.fromValue(role),
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tokenInvalidBefore = tokenInvalidBefore,
)

private fun AuthUser.toStored(): StoredAuthUser = StoredAuthUser(
    username = username,
    passwordHash = passwordHash,
    role = role.claimValue,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tokenInvalidBefore = tokenInvalidBefore,
)
