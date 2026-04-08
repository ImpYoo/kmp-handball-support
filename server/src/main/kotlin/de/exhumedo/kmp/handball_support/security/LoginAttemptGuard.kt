package de.exhumedo.kmp.handball_support.security

import de.exhumedo.kmp.handball_support.auth.AuthenticationThrottledException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Guards token issuance against repeated failed login attempts.
 *
 * @property clock Clock used for window and cooldown calculations.
 * @property maxFailedAttempts Number of failed attempts allowed before throttling.
 * @property failureWindow Time window for counting failures.
 * @property blockDuration Cooldown duration once throttled.
 */
class LoginAttemptGuard(
    private val clock: Clock = Clock.System,
    private val maxFailedAttempts: Int = 5,
    private val failureWindow: Duration = 10.minutes,
    private val blockDuration: Duration = 15.minutes,
) {
    private val lock = Any()
    private val states = mutableMapOf<String, AttemptState>()

    /**
     * Verifies that the username is currently allowed to attempt authentication.
     *
     * @param username Username being authenticated.
     * @throws AuthenticationThrottledException When the user is temporarily blocked.
     */
    fun checkAllowed(username: String) {
        synchronized(lock) {
            val now = clock.now()
            val state = states[username] ?: return
            val blockedUntil = state.blockedUntil
            if (blockedUntil != null && now < blockedUntil) {
                val remainingMillis = (blockedUntil - now).inWholeMilliseconds
                val retryAfterSeconds = ((remainingMillis + 999) / 1000).coerceAtLeast(1)
                throw AuthenticationThrottledException(username, retryAfterSeconds)
            }

            if (blockedUntil != null && now >= blockedUntil) {
                states.remove(username)
            }
        }
    }

    /**
     * Records a failed authentication attempt.
     *
     * @param username Username that failed to authenticate.
     */
    fun recordFailure(username: String) {
        synchronized(lock) {
            val now = clock.now()
            val existing = states[username]
            val failureCount = if (existing == null || now - existing.firstFailureAt > failureWindow) {
                1
            } else {
                existing.failureCount + 1
            }

            val firstFailureAt = if (existing == null || now - existing.firstFailureAt > failureWindow) {
                now
            } else {
                existing.firstFailureAt
            }

            val blockedUntil = if (failureCount >= maxFailedAttempts) {
                now + blockDuration
            } else {
                null
            }

            states[username] = AttemptState(
                firstFailureAt = firstFailureAt,
                failureCount = failureCount,
                blockedUntil = blockedUntil,
            )
        }
    }

    /**
     * Clears any recorded failures after successful authentication.
     *
     * @param username Username that authenticated successfully.
     */
    fun recordSuccess(username: String) {
        synchronized(lock) {
            states.remove(username)
        }
    }
}

private data class AttemptState(
    val firstFailureAt: kotlin.time.Instant,
    val failureCount: Int,
    val blockedUntil: kotlin.time.Instant?,
)
