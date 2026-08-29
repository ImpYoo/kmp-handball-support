package de.exhumedo.kmp.handball_support.application.coaching

import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingGame
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingHistoryEntry
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingPerson

/**
 * Application command for creating a referee-coaching evaluation.
 */
data class CreateCoachingEvaluationCommand(
    val game: CoachingGame,
    val evaluatorUsername: String,
    val firstReferee: CoachingPerson,
    val secondReferee: CoachingPerson,
    val rootCauseCounts: Map<String, Map<String, Map<String, Int>>>,
    val comment: String,
    val history: List<CoachingHistoryEntry> = emptyList(),
)
