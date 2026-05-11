package de.exhumedo.kmp.handball_support.domain.rating.repository

import de.exhumedo.kmp.handball_support.domain.rating.model.PerformanceEvaluation

/**
 * Read/write access to persisted performance evaluations.
 *
 * Implementations are responsible for thread safety.
 * All methods are synchronous — I/O blocking is an implementation concern.
 */
interface PerformanceEvaluationRepository {
    fun save(evaluation: PerformanceEvaluation): PerformanceEvaluation
    fun findById(id: String): PerformanceEvaluation?
    fun findAll(): List<PerformanceEvaluation>
    fun findByGameId(gameId: String): List<PerformanceEvaluation>
    fun findByRefereeTeam(firstRefereeId: String, secondRefereeId: String): List<PerformanceEvaluation>
    fun findByDelegate(delegateId: String): List<PerformanceEvaluation>
}
