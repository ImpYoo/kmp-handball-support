package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Aggregate root representing one completed evaluation of a table official team.
 *
 * The aggregate belongs to this application and captures a referee pair's
 * evaluation of one table official team for one game. The domain rule of one
 * evaluation per game is enforced at repository level, not inside this class.
 * This model intentionally has no draft lifecycle: an evaluation is created
 * only once it is complete and ready to persist.
 *
 * @property id Unique evaluation identifier supplied by the caller.
 * @property game External game reference being evaluated.
 * @property refereePair Referee pair acting as the collective voter.
 * @property tableOfficialTeam Table official team that is evaluated.
 * @property score Completed evaluation score.
 * @property comment Optional free-text comment attached to the evaluation.
 * @property createdAt ISO 8601 timestamp string marking when the evaluation was recorded.
 */
class PerformanceEvaluation(
    val id: String,
    val game: Game,
    val refereePair: RefereePair,
    val tableOfficialTeam: TableOfficialTeam,
    val score: EvaluationScore,
    val comment: String?,
    val createdAt: String,
) {
    init {
        require(id.isNotBlank()) { "PerformanceEvaluation id must not be blank" }
        require(createdAt.isNotBlank()) { "PerformanceEvaluation createdAt must not be blank" }

        val overlappingPerson = refereePair.persons
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
            "referees=${refereePair.persons.size}, tableOfficials=${tableOfficialTeam.members.size})"
    }

    companion object {
        /**
         * Creates a completed evaluation owned by this application.
         *
         * @param id Unique evaluation identifier. When omitted, a random UUID is generated.
         * @param game External game reference being evaluated.
         * @param refereePair Referee pair acting as the collective voter.
         * @param tableOfficialTeam Table official team being evaluated.
         * @param score Completed evaluation score.
         * @param comment Optional free-text comment.
         * @param clock Clock used to derive the creation timestamp. Defaults to the system clock.
         * @return A complete evaluation aggregate ready to persist.
         */
        @OptIn(ExperimentalUuidApi::class)
        fun create(
            id: String = Uuid.random().toString(),
            game: Game,
            refereePair: RefereePair,
            tableOfficialTeam: TableOfficialTeam,
            score: EvaluationScore,
            comment: String?,
            clock: Clock = Clock.System,
        ): PerformanceEvaluation {
            return PerformanceEvaluation(
                id = id,
                game = game,
                refereePair = refereePair,
                tableOfficialTeam = tableOfficialTeam,
                score = score,
                comment = comment,
                createdAt = clock.now().toString(),
            )
        }
    }
}
