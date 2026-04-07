package de.exhumedo.kmp.handball_support.application

import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Game
import de.exhumedo.kmp.handball_support.domain.rating.model.RefereePair
import de.exhumedo.kmp.handball_support.domain.rating.model.TableOfficialTeam

/**
 * Application command for creating a new performance evaluation.
 *
 * @property game Referenced game.
 * @property refereePair Referee pair acting as evaluator.
 * @property tableOfficialTeam Evaluated table official team.
 * @property score Raw evaluation criteria.
 * @property comment Optional free-text comment.
 */
data class CreatePerformanceEvaluationCommand(
    val game: Game,
    val refereePair: RefereePair,
    val tableOfficialTeam: TableOfficialTeam,
    val score: EvaluationScore,
    val comment: String?,
)
