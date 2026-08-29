package de.exhumedo.kmp.handball_support.referee_coaching.domain.model

/**
 * One row in the result presentation: a criterion with its final score,
 * the deductions applied, and the selected root-cause names per group.
 */
data class CriterionReportRow(
    val criterionId: String,
    val criterionName: String,
    val category: CriterionCategory,
    val score: Int,
    val maxScore: Int,
    val deductionPoints: Int,
    val defectGroups: List<DefectGroupReportRow>,
)

/**
 * Root-cause detail inside a [CriterionReportRow].
 */
data class DefectGroupReportRow(
    val groupId: String,
    val groupName: String,
    val selectedRootCauses: List<RootCauseReportRow>,
)

/**
 * A selected root cause with its count and the readable name.
 */
data class RootCauseReportRow(
    val rootCauseId: String,
    val rootCauseName: String,
    val count: Int,
)

/**
 * Computed result presentation for a single [RefereeCoachingEvaluation].
 */
data class RefereeCoachingReport(
    val evaluationId: String,
    val game: CoachingGame,
    val firstReferee: CoachingPerson,
    val secondReferee: CoachingPerson,
    val evaluatorUsername: String,
    val comment: String,
    val rows: List<CriterionReportRow>,
    val totalScore: Int,
    val maxTotalScore: Int,
    val percentage: Int,
)
