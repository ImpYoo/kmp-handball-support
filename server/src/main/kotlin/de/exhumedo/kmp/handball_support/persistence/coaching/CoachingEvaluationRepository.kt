package de.exhumedo.kmp.handball_support.persistence.coaching

import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingEvaluation

/**
 * Persistence contract for referee-coaching evaluations.
 */
interface CoachingEvaluationRepository {

    /** Saves or updates an evaluation and returns the stored aggregate. */
    fun save(evaluation: RefereeCoachingEvaluation): RefereeCoachingEvaluation

    /** Loads a single evaluation by its server-generated id, or null. */
    fun findById(id: String): RefereeCoachingEvaluation?

    /** Lists evaluations, optionally filtered. */
    fun findAll(filter: CoachingEvaluationFilter = CoachingEvaluationFilter()): List<RefereeCoachingEvaluation>

    /** Deletes an evaluation. Returns true if it existed. */
    fun deleteById(id: String): Boolean
}

/**
 * Filter for listing coaching evaluations. All fields are AND-combined.
 */
data class CoachingEvaluationFilter(
    val gameId: String? = null,
    val refereePersonId: String? = null,
    val evaluatorUsername: String? = null,
    val from: String? = null,
    val to: String? = null,
)
