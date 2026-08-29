package de.exhumedo.kmp.handball_support.auth

import kotlinx.serialization.Serializable

@Serializable
data class StoredAuth(
    val token: String,
    val role: String? = null,
    val username: String? = null,
)

