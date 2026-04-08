package de.exhumedo.kmp.handball_support.domain.rating.repository

import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluatorReference
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluatorType
import de.exhumedo.kmp.handball_support.domain.rating.model.PerformanceEvaluation

/**
 * Domain port for persisting and retrieving performance evaluations.
 *
 * The interface lives in the domain layer while concrete adapters live in
 * infrastructure. The `suspend` keyword is used only as a Kotlin language
 * feature so implementations can support asynchronous I/O without introducing
 * coroutine library imports into the domain model.
 *
 * Implementations are responsible for enforcing the uniqueness rule that only
 * one evaluation may exist for a given game and evaluator type.
 */
interface PerformanceEvaluationRepository {

    /**
     * Persists a new evaluation or updates an existing one.
     *
     * @param evaluation The aggregate to save.
     * @return The saved aggregate as stored by the implementation.
     */
    suspend fun save(evaluation: PerformanceEvaluation): PerformanceEvaluation

    /**
     * Finds an evaluation by its domain identifier.
     *
     * @param id The evaluation identifier.
     * @return The matching aggregate or `null` when none exists.
     */
    suspend fun findById(id: String): PerformanceEvaluation?

    /**
     * Finds evaluations associated with one game.
     *
     * @param gameId The external game identifier.
     * @return All matching aggregates for that game.
     */
    suspend fun findByGameId(gameId: String): List<PerformanceEvaluation>

    /**
     * Finds all evaluations created by the same evaluator reference.
     *
     * @param evaluatorReference Evaluator-side query reference.
     * @return All matching evaluations for that evaluator reference.
     */
    suspend fun findByEvaluatorReference(
        evaluatorReference: EvaluatorReference,
    ): List<PerformanceEvaluation>

    /**
     * Checks whether an evaluation already exists for the specified game and evaluator type.
     *
     * @param gameId The external game identifier.
     * @param evaluatorType Stable evaluator kind.
     * @return `true` when the game already has an evaluation for that evaluator type, otherwise `false`.
     */
    suspend fun existsByGameIdAndEvaluatorType(
        gameId: String,
        evaluatorType: EvaluatorType,
    ): Boolean

    /**
     * Returns every stored evaluation.
     *
     * @return All persisted evaluations.
     */
    suspend fun findAll(): List<PerformanceEvaluation>
}
