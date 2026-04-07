package de.exhumedo.kmp.handball_support.api.dto

import kotlinx.serialization.Serializable

/**
 * RFC 7807 style problem response used by the API.
 *
 * @property type Problem type URI.
 * @property title Short problem title.
 * @property status HTTP status code.
 * @property detail Human-readable problem detail.
 * @property instance Request path for which the problem occurred.
 */
@Serializable
data class ProblemDto(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String,
)
