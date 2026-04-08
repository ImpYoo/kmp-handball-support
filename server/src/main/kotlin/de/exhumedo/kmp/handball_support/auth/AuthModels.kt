package de.exhumedo.kmp.handball_support.auth

/**
 * Supported authorization roles for API users.
 *
 * @property claimValue JWT claim value and transport representation for the role.
 */
enum class AuthRole(
    val claimValue: String,
) {
    /** Full administrative access. */
    ADMIN("admin"),

    /** Referee access for creating and reading evaluations. */
    REFEREE("referee"),

    /** Read-only access. */
    VIEWER("viewer"), ;

    companion object {
        /**
         * Resolves a role from a JWT claim or persisted string.
         *
         * @param value Raw role value.
         * @return Matching role.
         * @throws IllegalArgumentException When the value does not represent a supported role.
         */
        fun fromValue(value: String): AuthRole {
            return entries.firstOrNull { it.claimValue == value.trim().lowercase() }
                ?: throw IllegalArgumentException("Unsupported auth role '$value'.")
        }
    }
}

/**
 * Persisted authentication user.
 *
 * @property username Unique normalized login name.
 * @property passwordHash PBKDF2 password hash including algorithm metadata and salt.
 * @property role Granted authorization role.
 * @property enabled Whether the account may authenticate.
 * @property createdAt ISO-8601 timestamp when the user was created.
 * @property updatedAt ISO-8601 timestamp when the user was last modified.
 * @property tokenInvalidBefore Optional ISO-8601 cutoff. Tokens issued before this instant are rejected.
 */
data class AuthUser(
    val username: String,
    val passwordHash: String,
    val role: AuthRole,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val tokenInvalidBefore: String? = null,
)

/**
 * Minimal bootstrap credentials used to create the first administrative account
 * when the JSON user store is empty.
 *
 * @property username Initial admin username.
 * @property password Initial admin password in plain text before hashing.
 */
data class BootstrapAdmin(
    val username: String,
    val password: String,
)

/**
 * Config object for JWT issuing and verification.
 *
 * @property issuer JWT issuer.
 * @property audience JWT audience.
 * @property realm Authentication realm.
 * @property secret HMAC signing secret.
 * @property tokenTtlSeconds Access token lifetime in seconds.
 */
data class JwtConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    val secret: String,
    val tokenTtlSeconds: Long,
)

/**
 * Contract for authenticating and administrating API users.
 */
interface AuthUserStore {
    /**
     * Authenticates a username/password pair.
     *
     * @param username Provided username.
     * @param password Provided plain-text password.
     * @return Authenticated user or `null` when credentials are invalid.
     */
    fun authenticate(username: String, password: String): AuthUser?

    /**
     * Returns all stored users.
     *
     * @return Users sorted by username.
     */
    fun findAll(): List<AuthUser>

    /**
     * Finds a single user by username.
     *
     * @param username Username to resolve.
     * @return Matching user or `null`.
     */
    fun findByUsername(username: String): AuthUser?

    /**
     * Creates and persists a new user.
     *
     * @param username Requested username.
     * @param password Plain-text password to hash before storage.
     * @param role Granted authorization role.
     * @param enabled Initial enabled state.
     * @return Stored user.
     */
    fun createUser(
        username: String,
        password: String,
        role: AuthRole,
        enabled: Boolean = true,
    ): AuthUser

    /**
     * Updates an existing user.
     *
     * @param username Target username.
     * @param password Optional replacement plain-text password.
     * @param role Optional replacement role.
     * @param enabled Optional replacement enabled state.
     * @return Updated user.
     */
    fun updateUser(
        username: String,
        password: String? = null,
        role: AuthRole? = null,
        enabled: Boolean? = null,
    ): AuthUser

    /**
     * Revokes all currently active tokens for a user by setting a new invalid-before cutoff.
     *
     * @param username Username whose tokens should be invalidated.
     * @param invalidBefore ISO-8601 timestamp. Tokens issued before this instant become unusable.
     * @return Updated user after the revocation marker has been stored.
     */
    fun revokeTokens(
        username: String,
        invalidBefore: String,
    ): AuthUser

    /**
     * Deletes a user permanently.
     *
     * @param username Username to delete.
     */
    fun deleteUser(username: String)
}
