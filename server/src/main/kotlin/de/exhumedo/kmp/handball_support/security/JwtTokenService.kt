package de.exhumedo.kmp.handball_support.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUser
import de.exhumedo.kmp.handball_support.auth.JwtConfig
import java.util.Date
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Issues and verifies JWT access tokens.
 *
 * @property config JWT configuration values.
 * @property clock Clock used for issuance and expiry timestamps.
 */
class JwtTokenService(
    private val config: JwtConfig,
    private val clock: Clock = Clock.System,
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(config.secret)

    /**
     * JWT verifier used by request security.
     */
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    /**
     * Creates a signed JWT for the supplied authenticated user.
     *
     * @param user Authenticated user.
     * @return Token payload including the signed JWT and its expiry timestamp.
     */
    fun issueToken(user: AuthUser): IssuedToken {
        val issuedAt = clock.now()
        val expiresAt = issuedAt + config.tokenTtlSeconds.seconds

        val token = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(user.username)
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("role", user.role.claimValue)
            .withIssuedAt(Date(issuedAt.toEpochMilliseconds()))
            .withExpiresAt(Date(expiresAt.toEpochMilliseconds()))
            .sign(algorithm)

        return IssuedToken(
            token = token,
            expiresAt = expiresAt.toString(),
            role = user.role.claimValue,
        )
    }

    /**
     * Verifies a signed JWT and extracts the authenticated subject and role.
     *
     * @param token Signed JWT access token.
     * @return Extracted token claims when verification succeeds, otherwise `null`.
     */
    fun verify(token: String): VerifiedToken? {
        val decoded = runCatching { verifier.verify(token) }.getOrNull() ?: return null
        val subject = decoded.subject?.takeIf { it.isNotBlank() } ?: return null
        val roleClaim = decoded.getClaim("role").asString()?.takeIf { it.isNotBlank() } ?: return null
        val role = runCatching { AuthRole.fromValue(roleClaim) }.getOrNull() ?: return null
        val issuedAt = decoded.issuedAt?.toInstant()?.toString() ?: return null

        return VerifiedToken(
            subject = subject,
            role = role,
            issuedAt = issuedAt,
        )
    }
}

/**
 * Value object representing a newly issued JWT.
 *
 * @property token Signed JWT string.
 * @property expiresAt ISO 8601 expiry timestamp.
 * @property role Role embedded into the token.
 */
data class IssuedToken(
    val token: String,
    val expiresAt: String,
    val role: String,
)

/**
 * Verified JWT claims used for route authorization.
 *
 * @property subject Authenticated user identity.
 * @property role Granted application role.
 * @property issuedAt ISO-8601 issue timestamp.
 */
data class VerifiedToken(
    val subject: String,
    val role: AuthRole,
    val issuedAt: String,
)
