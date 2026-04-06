package de.exhumedo.kmp.handball_support.domain.rating.repository

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
 * one evaluation may exist for a given game.
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
     * Finds the unique evaluation associated with one game.
     *
     * @param gameId The external game identifier.
     * @return The matching aggregate or `null` when none exists.
     */
    suspend fun findByGameId(gameId: String): PerformanceEvaluation?

    /**
     * Finds all evaluations created by the same referee pair.
     *
     * @param firstRefereeId The first referee person identifier.
     * @param secondRefereeId The second referee person identifier.
     * @return All matching evaluations for that pair of referee persons.
     */
    suspend fun findByRefereePairPersonIds(
        firstRefereeId: String,
        secondRefereeId: String,
    ): List<PerformanceEvaluation>

    /**
     * Checks whether an evaluation already exists for the specified game.
     *
     * @param gameId The external game identifier.
     * @return `true` when the game already has an evaluation, otherwise `false`.
     */
    suspend fun existsByGameId(gameId: String): Boolean

    /**
     * Returns every stored evaluation.
     *
     * @return All persisted evaluations.
     */
    suspend fun findAll(): List<PerformanceEvaluation>
}
