package de.exhumedo.kmp.handball_support.domain.repository

import de.exhumedo.kmp.handball_support.domain.model.ApiEnvelope

data class IdempotencyRecord(
    val key: String,
    val requestHash: String,
    val response: ApiEnvelope,
)

interface IdempotencyRepository {
    fun find(key: String): IdempotencyRecord?
    fun save(record: IdempotencyRecord)
}

