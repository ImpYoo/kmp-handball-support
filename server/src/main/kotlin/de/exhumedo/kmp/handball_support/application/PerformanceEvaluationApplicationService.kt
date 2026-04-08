package de.exhumedo.kmp.handball_support.application

import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Evaluator
import de.exhumedo.kmp.handball_support.domain.rating.model.Game
import de.exhumedo.kmp.handball_support.domain.rating.model.PerformanceEvaluation
import de.exhumedo.kmp.handball_support.domain.rating.model.TableOfficialTeam
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Application service orchestrating creation of performance evaluations.
 *
 * @property repository Repository used to persist the aggregate.
 * @property idGenerator Generator for new evaluation identifiers.
 * @property clock Clock used for derived creation timestamps.
 */
class PerformanceEvaluationApplicationService(
    private val repository: PerformanceEvaluationRepository,
    private val idGenerator: EvaluationIdGenerator = UuidEvaluationIdGenerator(),
    private val clock: Clock = Clock.System,
) {
    /**
     * Creates and stores a new evaluation.
     *
     * @param game Referenced game.
     * @param evaluator Allowed evaluator acting as voter.
     * @param tableOfficialTeam Evaluated table official team.
     * @param score Raw evaluation score.
     * @param comment Optional free-text comment.
     * @return The saved aggregate.
     */
    suspend fun create(
        game: Game,
        evaluator: Evaluator,
        tableOfficialTeam: TableOfficialTeam,
        score: EvaluationScore,
        comment: String?,
    ): PerformanceEvaluation {
        val evaluation = PerformanceEvaluation.create(
            id = idGenerator.newId(),
            game = game,
            evaluator = evaluator,
            tableOfficialTeam = tableOfficialTeam,
            score = score,
            comment = comment,
            clock = clock,
        )

        return repository.save(evaluation)
    }
}

/**
 * Abstraction for generating new evaluation identifiers.
 */
fun interface EvaluationIdGenerator {
    /**
     * Generates a new identifier.
     *
     * @return A unique identifier string.
     */
    fun newId(): String
}

/**
 * UUID-based evaluation identifier generator.
 */
class UuidEvaluationIdGenerator : EvaluationIdGenerator {
    /**
     * Generates a random stdlib UUID string.
     *
     * @return A new UUID string.
     */
    @OptIn(ExperimentalUuidApi::class)
    override fun newId(): String = Uuid.random().toString()
}
