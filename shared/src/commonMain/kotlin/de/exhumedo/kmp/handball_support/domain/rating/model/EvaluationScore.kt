package de.exhumedo.kmp.handball_support.domain.rating.model

/**
 * Fixed evaluation criteria used by the table official assessment.
 *
 * The criteria are part of the domain model and intentionally fixed at compile
 * time. They are not dynamic, configurable, or database-driven. The raw
 * criterion scores are persisted as entered, while weighted totals are derived
 * later from those stored values so federation weighting rules can evolve
 * without changing historical inputs.
 *
 * @property appearance Score for the officials' appearance and professional presence.
 * @property influence Score for the officials' impact on the game process.
 * @property teamwork Score for how well the table team works together.
 */
data class EvaluationScore(
    val appearance: Score,
    val influence: Score,
    val teamwork: Score,
) {
    /**
     * Calculates a weighted total score across the fixed criteria.
     *
     * @param appearanceWeight Weight for the appearance criterion.
     * @param influenceWeight Weight for the influence criterion.
     * @param teamworkWeight Weight for the teamwork criterion.
     * @return The aggregated weighted total.
     */
    fun toScore(
        appearanceWeight: Int = 1,
        influenceWeight: Int = 1,
        teamworkWeight: Int = 1,
    ): Int {
        require(appearanceWeight >= 0) { "appearanceWeight must be greater than or equal to zero" }
        require(influenceWeight >= 0) { "influenceWeight must be greater than or equal to zero" }
        require(teamworkWeight >= 0) { "teamworkWeight must be greater than or equal to zero" }

        return appearance.value * appearanceWeight +
            influence.value * influenceWeight +
            teamwork.value * teamworkWeight
    }
}
