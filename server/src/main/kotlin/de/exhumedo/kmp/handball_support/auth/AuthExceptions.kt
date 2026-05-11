package de.exhumedo.kmp.handball_support.auth

/**
 * Raised when an auth user already exists.
 *
 * @property username Conflicting username.
 */
class AuthUserAlreadyExistsException(
    val username: String,
) : IllegalStateException("Auth user '$username' already exists.")

/**
 * Raised when an auth user cannot be found.
 *
 * @property username Missing username.
 */
class AuthUserNotFoundException(
    val username: String,
) : NoSuchElementException("Auth user '$username' was not found.")

/**
 * Raised when an update or delete operation would remove the last enabled admin.
 *
 * @property username Target username.
 */
class LastEnabledAdminRemovalException(
    val username: String,
) : IllegalStateException("User '$username' is the last enabled admin and cannot be removed or downgraded.")

/**
 * Raised when the auth user store cannot bootstrap itself securely.
 */
class AuthBootstrapException(
    message: String,
) : IllegalStateException(message)

/**
 * Raised when a user provides an incorrect current password during a self-service password change.
 *
 * @property username Username that failed verification.
 */
class AuthenticationFailedException(
    val username: String,
) : IllegalStateException("Current password is incorrect for user '$username'.")

/**
 * Raised when repeated failed login attempts trigger a temporary authentication block.
 *
 * @property username Throttled username.
 * @property retryAfterSeconds Suggested wait time before retrying.
 */
class AuthenticationThrottledException(
    val username: String,
    val retryAfterSeconds: Long,
) : IllegalStateException(
    "Authentication for user '$username' is temporarily throttled. Retry after $retryAfterSeconds seconds.",
)
