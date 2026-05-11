package de.exhumedo.kmp.handball_support.domain.rating.model

data class PerformanceEvaluation(
    val id: String,
    val game: Game,
    val evaluator: Evaluator,
    val tableOfficialTeam: TableOfficialTeam,
    val score: EvaluationScore,
    val comment: String,
    val createdAt: String,
) {
    companion object {
        const val MAX_COMMENT_LENGTH = 500
    }
}
