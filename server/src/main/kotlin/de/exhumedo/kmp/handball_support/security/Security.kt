package de.exhumedo.kmp.handball_support.security

import de.exhumedo.kmp.handball_support.api.respondProblem
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.auth.JwtConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey
import kotlin.time.Instant

/**
 * Performs startup-time validation for JWT security configuration.
 *
 * @param jwtConfig JWT configuration values.
 * @param tokenService Token service used for issuing and verifying tokens.
 */
fun Application.configureSecurity(
    jwtConfig: JwtConfig,
    tokenService: JwtTokenService,
) {
    require(jwtConfig.secret.isNotBlank()) { "JWT secret must not be blank" }
    require(jwtConfig.issuer.isNotBlank()) { "JWT issuer must not be blank" }
    require(jwtConfig.audience.isNotBlank()) { "JWT audience must not be blank" }
    require(jwtConfig.realm.isNotBlank()) { "JWT realm must not be blank" }
    require(jwtConfig.tokenTtlSeconds > 0) { "JWT token TTL must be greater than zero" }

    tokenService.verifier
}

/**
 * Verifies the current request's bearer token against the allowed roles.
 *
 * @param tokenService Token service used to verify incoming JWTs.
 * @param roles Allowed roles for the current endpoint.
 * @return The verified token when the request is authenticated and authorized, otherwise `null`.
 */
@Suppress("ReturnCount")
suspend fun ApplicationCall.authorize(
    tokenService: JwtTokenService,
    authUserStore: AuthUserStore,
    vararg roles: AuthRole,
): VerifiedToken? {
    val authorizationHeader = request.headers[HttpHeaders.Authorization]
    val token = extractBearerToken(authorizationHeader)

    if (token == null) {
        respondProblem(
            status = HttpStatusCode.Unauthorized,
            title = "Unauthorized",
            detail = "Token is missing, invalid, or expired.",
        )
        return null
    }

    val verifiedToken = tokenService.verify(token)
    if (verifiedToken == null) {
        respondProblem(
            status = HttpStatusCode.Unauthorized,
            title = "Unauthorized",
            detail = "Token is missing, invalid, or expired.",
        )
        return null
    }

    val currentUser = authUserStore.findByUsername(verifiedToken.subject)
    if (currentUser == null || !currentUser.enabled || currentUser.role != verifiedToken.role) {
        respondProblem(
            status = HttpStatusCode.Unauthorized,
            title = "Unauthorized",
            detail = "Token is missing, invalid, or expired.",
        )
        return null
    }

    if (isTokenRevoked(verifiedToken.issuedAt, currentUser.tokenInvalidBefore)) {
        respondProblem(
            status = HttpStatusCode.Unauthorized,
            title = "Unauthorized",
            detail = "Token is missing, invalid, or expired.",
        )
        return null
    }

    if (verifiedToken.role !in roles) {
        respondProblem(
            status = HttpStatusCode.Forbidden,
            title = "Forbidden",
            detail = "Role '${verifiedToken.role.claimValue}' is not allowed to access this resource.",
        )
        return null
    }

    attributes.put(verifiedTokenKey, verifiedToken)
    return verifiedToken
}

private fun isTokenRevoked(
    issuedAt: String,
    tokenInvalidBefore: String?,
): Boolean {
    if (tokenInvalidBefore == null) {
        return false
    }

    val issuedAtInstant = runCatching { Instant.parse(issuedAt) }.getOrNull() ?: return true
    val invalidBeforeInstant = runCatching { Instant.parse(tokenInvalidBefore) }.getOrNull() ?: return true
    return issuedAtInstant <= invalidBeforeInstant
}

/**
 * Returns the verified token attached during authorization, if present.
 *
 * @return Verified token for the current request or `null`.
 */
fun ApplicationCall.verifiedTokenOrNull(): VerifiedToken? = attributes.getOrNull(verifiedTokenKey)

private fun extractBearerToken(authorizationHeader: String?): String? {
    if (authorizationHeader == null) {
        return null
    }

    val prefix = "Bearer "
    return if (authorizationHeader.startsWith(prefix, ignoreCase = true)) {
        authorizationHeader.substring(prefix.length).trim().takeIf { it.isNotBlank() }
    } else {
        null
    }
}

private val verifiedTokenKey = AttributeKey<VerifiedToken>("verified-token")
