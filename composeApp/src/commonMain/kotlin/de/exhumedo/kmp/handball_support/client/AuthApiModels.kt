package de.exhumedo.kmp.handball_support.client

import kotlinx.serialization.Serializable

@Serializable
data class AuthUserResponseDto(
    val username: String,
    val role: String,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val tokenInvalidBefore: String? = null,
)

@Serializable
data class CreateAuthUserRequestDto(
    val username: String,
    val password: String,
    val role: String,
    val enabled: Boolean = true,
)

@Serializable
data class UpdateAuthUserRequestDto(
    val password: String? = null,
    val role: String? = null,
    val enabled: Boolean? = null,
)

@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String,
)
