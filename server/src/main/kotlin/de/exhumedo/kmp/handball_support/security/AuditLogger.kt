package de.exhumedo.kmp.handball_support.security

import de.exhumedo.kmp.handball_support.auth.AuthRole
import org.slf4j.LoggerFactory

/**
 * Emits security-relevant audit events.
 */
interface AuthAuditLogger {
    /**
     * Records a successful authentication.
     *
     * @param username Authenticated username.
     * @param role Granted role.
     */
    fun loginSucceeded(username: String, role: AuthRole)

    /**
     * Records a failed authentication attempt.
     *
     * @param username Attempted username.
     */
    fun loginFailed(username: String)

    /**
     * Records a created auth user.
     *
     * @param actorUsername Acting admin username.
     * @param createdUsername Created username.
     * @param role Granted role.
     */
    fun userCreated(actorUsername: String, createdUsername: String, role: AuthRole)

    /**
     * Records an updated auth user.
     *
     * @param actorUsername Acting admin username.
     * @param updatedUsername Updated username.
     */
    fun userUpdated(actorUsername: String, updatedUsername: String)

    /**
     * Records a deleted auth user.
     *
     * @param actorUsername Acting admin username.
     * @param deletedUsername Deleted username.
     */
    fun userDeleted(actorUsername: String, deletedUsername: String)

    /**
     * Records token revocation for a user.
     *
     * @param actorUsername Acting username that initiated the revocation.
     * @param subjectUsername Username whose active tokens were revoked.
     */
    fun tokensRevoked(actorUsername: String, subjectUsername: String)
}

/**
 * SLF4J-based audit logger for auth events.
 */
class Slf4jAuthAuditLogger : AuthAuditLogger {
    private val logger = LoggerFactory.getLogger(Slf4jAuthAuditLogger::class.java)

    /**
     * Logs a successful login.
     */
    override fun loginSucceeded(username: String, role: AuthRole) {
        logger.info("auth.login.succeeded username={} role={}", username, role.claimValue)
    }

    /**
     * Logs a failed login attempt.
     */
    override fun loginFailed(username: String) {
        logger.warn("auth.login.failed username={}", username)
    }

    /**
     * Logs an auth user creation.
     */
    override fun userCreated(actorUsername: String, createdUsername: String, role: AuthRole) {
        logger.info(
            "auth.user.created actor={} subject={} role={}",
            actorUsername,
            createdUsername,
            role.claimValue,
        )
    }

    /**
     * Logs an auth user update.
     */
    override fun userUpdated(actorUsername: String, updatedUsername: String) {
        logger.info("auth.user.updated actor={} subject={}", actorUsername, updatedUsername)
    }

    /**
     * Logs an auth user deletion.
     */
    override fun userDeleted(actorUsername: String, deletedUsername: String) {
        logger.info("auth.user.deleted actor={} subject={}", actorUsername, deletedUsername)
    }

    /**
     * Logs a token revocation event.
     */
    override fun tokensRevoked(actorUsername: String, subjectUsername: String) {
        logger.info("auth.token.revoked actor={} subject={}", actorUsername, subjectUsername)
    }
}
