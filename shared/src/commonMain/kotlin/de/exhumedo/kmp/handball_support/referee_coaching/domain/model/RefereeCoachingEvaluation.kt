package de.exhumedo.kmp.handball_support.referee_coaching.domain.model

/**
 * Minimal person reference used inside the coaching domain.
 */
data class CoachingPerson(
    val personId: String,
    val firstName: String,
    val lastName: String,
)

/**
 * Match metadata embedded in a coaching evaluation.
 */
data class CoachingGame(
    val gameId: String,
    val matchDate: String,
    val homeTeam: String,
    val awayTeam: String,
)

/**
 * Aggregate root: one completed referee-coaching observation sheet.
 *
 * The sheet is always tied to a concrete match and referee pair. It contains
 * the criteria catalog with live root-cause counts, from which scores are
 * derived.
 */
data class RefereeCoachingEvaluation(
    val id: String,
    val game: CoachingGame,
    val evaluatorUsername: String,
    val firstReferee: CoachingPerson,
    val secondReferee: CoachingPerson,
    val criteria: List<Criterion>,
    val comment: String,
    val createdAt: String,
    val updatedAt: String,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(game.gameId.isNotBlank()) { "gameId must not be blank" }
        require(evaluatorUsername.isNotBlank()) { "evaluatorUsername must not be blank" }
        require(firstReferee.personId.isNotBlank()) { "firstReferee.personId must not be blank" }
        require(secondReferee.personId.isNotBlank()) { "secondReferee.personId must not be blank" }
    }

    val totalScore: Int get() = criteria.sumOf { it.score }
    val maxTotalScore: Int get() = criteria.size * ScoringConfig.DEFAULT_SCORE
    val percentage: Int get() = if (maxTotalScore == 0) 0 else (totalScore * 100 / maxTotalScore)

    /** True if at least one root cause has been selected. */
    val isStarted: Boolean get() = criteria.any { criterion ->
        criterion.defectGroups.any { group ->
            group.rootCauses.any { it.count != 0 }
        }
    }
}
