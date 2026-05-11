package de.exhumedo.kmp.handball_support.application

import de.exhumedo.kmp.handball_support.domain.rating.model.Evaluator
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Game
import de.exhumedo.kmp.handball_support.domain.rating.model.TableOfficialTeam

data class CreatePerformanceEvaluationCommand(
    val game: Game,
    val evaluator: Evaluator,
    val tableOfficialTeam: TableOfficialTeam,
    val score: EvaluationScore,
    val comment: String,
)
