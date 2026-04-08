package de.exhumedo.kmp.handball_support.auth

import kotlinx.serialization.Serializable

/**
 * Login request DTO used to obtain a JWT access token.
 *
 * @property username Username credential.
 * @property password Password credential.
 */
@Serializable
data class TokenRequestDto(
    val username: String,
    val password: String,
)

/**
 * JWT token response DTO.
 *
 * @property accessToken Signed JWT access token.
 * @property tokenType Token type, always `Bearer`.
 * @property expiresAt ISO 8601 timestamp when the token expires.
 * @property role Role granted to the authenticated user.
 */
@Serializable
data class TokenResponseDto(
    val accessToken: String,
    val tokenType: String,
    val expiresAt: String,
    val role: String,
)

/**
 * Transport role values for auth user administration.
 */
@Serializable
enum class AuthRoleDto {
    /** Administrative role. */
    ADMIN,

    /** Referee role. */
    REFEREE,

    /** Viewer role. */
    VIEWER,
}

/**
 * Request DTO for creating a new auth user.
 *
 * @property username Username to create.
 * @property password Initial password in plain text.
 * @property role Granted role.
 * @property enabled Initial enabled state.
 */
@Serializable
data class CreateAuthUserRequestDto(
    val username: String,
    val password: String,
    val role: AuthRoleDto,
    val enabled: Boolean = true,
)

/**
 * Request DTO for updating an existing auth user.
 *
 * @property password Optional replacement password.
 * @property role Optional replacement role.
 * @property enabled Optional replacement enabled state.
 */
@Serializable
data class UpdateAuthUserRequestDto(
    val password: String? = null,
    val role: AuthRoleDto? = null,
    val enabled: Boolean? = null,
)

/**
 * Response DTO for persisted auth users.
 *
 * @property username Normalized username.
 * @property role Granted role.
 * @property enabled Whether the account may authenticate.
 * @property createdAt Creation timestamp.
 * @property updatedAt Last update timestamp.
 * @property tokenInvalidBefore Optional token revocation cutoff for this user.
 */
@Serializable
data class AuthUserResponseDto(
    val username: String,
    val role: AuthRoleDto,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val tokenInvalidBefore: String? = null,
)

/**
 * Maps the transport role into the domain auth role.
 *
 * @return Matching auth role.
 */
fun AuthRoleDto.toDomain(): AuthRole = when (this) {
    AuthRoleDto.ADMIN -> AuthRole.ADMIN
    AuthRoleDto.REFEREE -> AuthRole.REFEREE
    AuthRoleDto.VIEWER -> AuthRole.VIEWER
}

/**
 * Maps a persisted auth user into its response DTO.
 *
 * @return Safe API response without the password hash.
 */
fun AuthUser.toResponseDto(): AuthUserResponseDto = AuthUserResponseDto(
    username = username,
    role = role.toDto(),
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tokenInvalidBefore = tokenInvalidBefore,
)

private fun AuthRole.toDto(): AuthRoleDto = when (this) {
    AuthRole.ADMIN -> AuthRoleDto.ADMIN
    AuthRole.REFEREE -> AuthRoleDto.REFEREE
    AuthRole.VIEWER -> AuthRoleDto.VIEWER
}
