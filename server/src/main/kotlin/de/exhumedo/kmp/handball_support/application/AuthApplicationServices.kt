package de.exhumedo.kmp.handball_support.application

import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUser
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.security.AuthAuditLogger
import de.exhumedo.kmp.handball_support.security.LoginAttemptGuard
import kotlin.time.Clock

/**
 * Application service orchestrating authentication and login-attempt throttling.
 *
 * @property userStore Auth user store used for credential verification.
 * @property attemptGuard Guard for repeated failed login attempts.
 * @property auditLogger Audit logger used for security events.
 */
class AuthenticationApplicationService(
    private val userStore: AuthUserStore,
    private val attemptGuard: LoginAttemptGuard,
    private val auditLogger: AuthAuditLogger,
) {
    /**
     * Authenticates a username/password pair.
     *
     * @param username Provided username.
     * @param password Provided password.
     * @return Authenticated user or `null` when credentials are invalid.
     */
    fun authenticate(username: String, password: String): AuthUser? {
        val normalizedUsername = username.trim().lowercase()
        attemptGuard.checkAllowed(normalizedUsername)

        val user = userStore.authenticate(normalizedUsername, password)
        if (user == null) {
            attemptGuard.recordFailure(normalizedUsername)
            auditLogger.loginFailed(normalizedUsername)
            return null
        }

        attemptGuard.recordSuccess(user.username)
        auditLogger.loginSucceeded(user.username, user.role)
        return user
    }
}

/**
 * Application service orchestrating admin auth-user management with audit logging.
 *
 * @property userStore Auth user store.
 * @property auditLogger Audit logger for admin operations.
 */
class AuthUserApplicationService(
    private val userStore: AuthUserStore,
    private val auditLogger: AuthAuditLogger,
    private val clock: Clock = Clock.System,
) {
    /**
     * Returns all persisted auth users.
     *
     * @return Persisted auth users.
     */
    fun findAll(): List<AuthUser> = userStore.findAll()

    /**
     * Finds a single auth user by username.
     *
     * @param username Username to find.
     * @return Matching auth user or `null`.
     */
    fun findByUsername(username: String): AuthUser? = userStore.findByUsername(username)

    /**
     * Creates a new auth user.
     *
     * @param actorUsername Acting admin username.
     * @param username Username to create.
     * @param password Plain-text password.
     * @param role Granted role.
     * @param enabled Initial enabled state.
     * @return Stored auth user.
     */
    fun createUser(
        actorUsername: String,
        username: String,
        password: String,
        role: AuthRole,
        enabled: Boolean,
    ): AuthUser {
        val created = userStore.createUser(
            username = username,
            password = password,
            role = role,
            enabled = enabled,
        )
        auditLogger.userCreated(actorUsername, created.username, created.role)
        return created
    }

    /**
     * Updates an existing auth user.
     *
     * @param actorUsername Acting admin username.
     * @param username Username to update.
     * @param password Optional replacement password.
     * @param role Optional replacement role.
     * @param enabled Optional replacement enabled state.
     * @return Updated auth user.
     */
    fun updateUser(
        actorUsername: String,
        username: String,
        password: String?,
        role: AuthRole?,
        enabled: Boolean?,
    ): AuthUser {
        val updated = userStore.updateUser(
            username = username,
            password = password,
            role = role,
            enabled = enabled,
        )
        auditLogger.userUpdated(actorUsername, updated.username)
        return updated
    }

    /**
     * Deletes an auth user.
     *
     * @param actorUsername Acting admin username.
     * @param username Username to delete.
     */
    fun deleteUser(
        actorUsername: String,
        username: String,
    ) {
        userStore.deleteUser(username)
        auditLogger.userDeleted(actorUsername, username.trim().lowercase())
    }

    /**
     * Revokes all currently active tokens for the target user.
     *
     * @param actorUsername Acting username initiating the revocation.
     * @param username Username whose tokens should be invalidated.
     * @return Updated auth user.
     */
    fun revokeTokens(
        actorUsername: String,
        username: String,
    ): AuthUser {
        val updated = userStore.revokeTokens(
            username = username,
            invalidBefore = clock.now().toString(),
        )
        auditLogger.tokensRevoked(actorUsername, updated.username)
        return updated
    }
}
