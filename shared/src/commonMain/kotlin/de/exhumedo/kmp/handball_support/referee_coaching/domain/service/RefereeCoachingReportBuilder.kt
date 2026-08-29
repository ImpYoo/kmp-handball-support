package de.exhumedo.kmp.handball_support.referee_coaching.domain.service

import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CriterionReportRow
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.DefectGroupReportRow
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingEvaluation
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingReport
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RootCauseReportRow
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.ScoringConfig

/**
 * Builds a human-readable result presentation from a stored coaching evaluation.
 *
 * The report re-derives scores from the stored root-cause counts using the
 * existing scoring rules, so the presentation cannot drift from the domain.
 */
class RefereeCoachingReportBuilder {

    fun build(evaluation: RefereeCoachingEvaluation): RefereeCoachingReport = RefereeCoachingReport(
        evaluationId = evaluation.id,
        game = evaluation.game,
        firstReferee = evaluation.firstReferee,
        secondReferee = evaluation.secondReferee,
        evaluatorUsername = evaluation.evaluatorUsername,
        comment = evaluation.comment,
        rows = evaluation.criteria.map { criterion ->
            val deductionPoints = ScoringConfig.DEFAULT_SCORE - criterion.score
            CriterionReportRow(
                criterionId = criterion.id,
                criterionName = criterion.name,
                category = criterion.category,
                score = criterion.score,
                maxScore = ScoringConfig.DEFAULT_SCORE,
                deductionPoints = deductionPoints,
                defectGroups = criterion.defectGroups.map { group ->
                    DefectGroupReportRow(
                        groupId = group.id,
                        groupName = group.name,
                        selectedRootCauses = group.rootCauses
                            .filter { it.count != 0 }
                            .map { rootCause ->
                                RootCauseReportRow(
                                    rootCauseId = rootCause.id,
                                    rootCauseName = rootCause.name,
                                    count = rootCause.count,
                                )
                            },
                    )
                },
            )
        },
        totalScore = evaluation.totalScore,
        maxTotalScore = evaluation.maxTotalScore,
        percentage = evaluation.percentage,
        history = evaluation.history,
    )
}
