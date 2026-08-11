package de.exhumedo.kmp.handball_support.referee_coaching.domain.model

/**
 * The two top-level sections of the HVNB referee observation sheet
 * ("Beobachterbericht").
 */
enum class CriterionCategory(val displayName: String) {
    /** Section A — "Spielregeln" (rules of the game). */
    RULES_OF_THE_GAME("Spielregeln"),

    /** Section B — "Persönlicher Eindruck" (personal impression). */
    PERSONAL_IMPRESSION("Persönlicher Eindruck"),
}

/**
 * A single referee evaluation criterion. Its [score] is reduced as root causes
 * are selected (incremented), and restored as they are deselected (decremented).
 *
 * The score is always kept within [ScoringConfig.minScore]..[ScoringConfig.maxScore].
 */
data class Criterion(
    val id: String,
    val name: String,
    val category: CriterionCategory = CriterionCategory.RULES_OF_THE_GAME,
    val score: Int = ScoringConfig.DEFAULT_SCORE,
    val defectGroups: List<DefectGroup> = emptyList(),
)

/** A group of related defects belonging to a [Criterion]. */
data class DefectGroup(
    val id: String,
    val name: String,
    val rootCauses: List<RootCause> = emptyList(),
)

/**
 * A concrete root cause within a [DefectGroup]. [count] tracks how often this
 * root cause has been observed and drives the incremental point deduction.
 */
data class RootCause(
    val id: String,
    val name: String,
    val count: Int = 0,
)

/**
 * Configuration for criterion scoring.
 *
 * Penalty thresholds grow incrementally: the first point is deducted after
 * [firstThreshold] increments, the next after [thresholdStep] more, and so on
 * (e.g. 3, then +5 = 8, then +7 = 15, ...).
 */
data class ScoringConfig(
    val minScore: Int = MIN_SCORE,
    val maxScore: Int = MAX_SCORE,
    val defaultScore: Int = DEFAULT_SCORE,
    val firstThreshold: Int = FIRST_THRESHOLD,
    val thresholdStep: Int = THRESHOLD_STEP,
) {
    init {
        require(minScore <= maxScore) { "minScore ($minScore) must be <= maxScore ($maxScore)." }
        require(defaultScore in minScore..maxScore) {
            "defaultScore ($defaultScore) must be within $minScore..$maxScore."
        }
        require(firstThreshold > 0) { "firstThreshold ($firstThreshold) must be > 0." }
        require(thresholdStep > 0) { "thresholdStep ($thresholdStep) must be > 0." }
    }

    companion object {
        const val MIN_SCORE = 0
        const val MAX_SCORE = 9
        const val DEFAULT_SCORE = 6
        const val FIRST_THRESHOLD = 3
        const val THRESHOLD_STEP = 2
    }
}

