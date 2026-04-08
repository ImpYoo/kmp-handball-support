package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import kotlin.time.Clock

/**
 * Aggregate root representing one completed evaluation of a table official team.
 *
 * The aggregate belongs to this application and captures one allowed evaluator's
 * assessment of one table official team for one game. The domain rule is one
 * evaluation per game and evaluator type. This model intentionally has no draft
 * lifecycle: an evaluation is created only once it is complete and ready to persist.
 *
 * @property id Unique evaluation identifier supplied by the caller.
 * @property game External game reference being evaluated.
 * @property evaluator Allowed evaluator acting as voter.
 * @property tableOfficialTeam Table official team that is evaluated.
 * @property score Completed evaluation score.
 * @property comment Optional free-text comment attached to the evaluation.
 * @property createdAt ISO 8601 timestamp string marking when the evaluation was recorded.
 */
class PerformanceEvaluation(
    val id: String,
    val game: Game,
    val evaluator: Evaluator,
    val tableOfficialTeam: TableOfficialTeam,
    val score: EvaluationScore,
    val comment: String?,
    val createdAt: String,
) {
    init {
        require(id.isNotBlank()) { "PerformanceEvaluation id must not be blank" }
        require(createdAt.isNotBlank()) { "PerformanceEvaluation createdAt must not be blank" }

        val overlappingPerson = evaluator.persons
            .map { it.id }
            .intersect(tableOfficialTeam.members.map { it.id }.toSet())
            .firstOrNull()

        if (overlappingPerson != null) {
            throw DomainException.DuplicatePersonInTeam(overlappingPerson)
        }
    }

    /**
     * Identity equality based solely on the aggregate identifier.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerformanceEvaluation) return false
        return id == other.id
    }

    /**
     * Hash code derived solely from the aggregate identifier.
     */
    override fun hashCode(): Int = id.hashCode()

    /**
     * Human-readable summary for logs and debugging.
     */
    override fun toString(): String {
        return "PerformanceEvaluation(id='$id', gameId='${game.gameId}', " +
            "evaluator=${evaluator.type}, tableOfficials=${tableOfficialTeam.members.size})"
    }

    companion object {
        /**
         * Creates a completed evaluation owned by this application.
         *
         * @param id Unique evaluation identifier.
         * @param game External game reference being evaluated.
         * @param evaluator Allowed evaluator acting as voter.
         * @param tableOfficialTeam Table official team being evaluated.
         * @param score Completed evaluation score.
         * @param comment Optional free-text comment.
         * @param clock Clock used to derive the creation timestamp. Defaults to the system clock.
         * @return A complete evaluation aggregate ready to persist.
         */
        fun create(
            id: String,
            game: Game,
            evaluator: Evaluator,
            tableOfficialTeam: TableOfficialTeam,
            score: EvaluationScore,
            comment: String?,
            clock: Clock = Clock.System,
        ): PerformanceEvaluation {
            return PerformanceEvaluation(
                id = id,
                game = game,
                evaluator = evaluator,
                tableOfficialTeam = tableOfficialTeam,
                score = score,
                comment = comment,
                createdAt = clock.now().toString(),
            )
        }
    }
}
