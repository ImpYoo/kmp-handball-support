package de.exhumedo.kmp.handball_support.application

import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Evaluator
import de.exhumedo.kmp.handball_support.domain.rating.model.Game
import de.exhumedo.kmp.handball_support.domain.rating.model.TableOfficialTeam

/**
 * Application command for creating a new performance evaluation.
 *
 * @property game Referenced game.
 * @property evaluator Allowed evaluator acting as voter.
 * @property tableOfficialTeam Evaluated table official team.
 * @property score Raw evaluation criteria.
 * @property comment Optional free-text comment.
 */
data class CreatePerformanceEvaluationCommand(
    val game: Game,
    val evaluator: Evaluator,
    val tableOfficialTeam: TableOfficialTeam,
    val score: EvaluationScore,
    val comment: String?,
)
