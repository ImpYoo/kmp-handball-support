package de.exhumedo.kmp.handball_support.domain.model

sealed interface ApiEnvelope

data class SuccessArrayEnvelope<T>(
    val success: Boolean = true,
    val values: List<T>,
) : ApiEnvelope

data class SuccessValueEnvelope<T>(
    val success: Boolean = true,
    val value: T,
) : ApiEnvelope

data class ErrorEnvelope(
    val success: Boolean = false,
    val message: String,
    val code: String? = null,
    val correlationId: String? = null,
) : ApiEnvelope
