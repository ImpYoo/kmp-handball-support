package de.exhumedo.kmp.handball_support.referee_coaching.domain.scoring

import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.ScoringConfig

/**
 * Applies incremental point deduction to a [Criterion] as root causes are
 * selected or deselected.
 *
 * The penalty for a single root cause is derived from its `count` using growing
 * thresholds: the first point is deducted after [ScoringConfig.firstThreshold]
 * increments, each subsequent point after [ScoringConfig.thresholdStep] more
 * (e.g. 3, then +5 = 8, then +7 = 15, ...).
 *
 * The mapping is **symmetric around zero**: a negative count yields a negative
 * penalty (a bonus) using the very same thresholds, so decrementing below zero
 * *increases* the criterion score by the same steps that incrementing would
 * decrease it.
 *
 * Scores are always coerced into [ScoringConfig.minScore]..[ScoringConfig.maxScore].
 */
class CriterionScoringService(
    private val config: ScoringConfig = ScoringConfig(),
) {

    /** Increments the matching root cause count by one and recomputes the score. */
    fun incrementRootCause(
        criterion: Criterion,
        groupId: String,
        rootCauseId: String,
    ): Criterion = updateRootCauseCount(criterion, groupId, rootCauseId, delta = 1)

    /** Decrements the matching root cause count by one (may go negative) and recomputes the score. */
    fun decrementRootCause(
        criterion: Criterion,
        groupId: String,
        rootCauseId: String,
    ): Criterion = updateRootCauseCount(criterion, groupId, rootCauseId, delta = -1)

    private fun updateRootCauseCount(
        criterion: Criterion,
        groupId: String,
        rootCauseId: String,
        delta: Int,
    ): Criterion {
        val oldPenalty = calculatePenalty(criterion)

        val updatedGroups = criterion.defectGroups.map { group ->
            if (group.id != groupId) return@map group

            group.copy(
                rootCauses = group.rootCauses.map { rootCause ->
                    if (rootCause.id != rootCauseId) {
                        rootCause
                    } else {
                        // Counts may go negative — the penalty mapping is symmetric.
                        rootCause.copy(count = rootCause.count + delta)
                    }
                },
            )
        }

        val updatedCriterion = criterion.copy(defectGroups = updatedGroups)

        val newPenalty = calculatePenalty(updatedCriterion)
        val scoreDelta = oldPenalty - newPenalty

        return updatedCriterion.copy(
            score = (criterion.score + scoreDelta).coerceIn(config.minScore, config.maxScore),
        )
    }

    private fun calculatePenalty(criterion: Criterion): Int =
        criterion.defectGroups.sumOf { group ->
            group.rootCauses.sumOf { rootCause -> penaltyForCount(rootCause.count) }
        }

    /**
     * Translates a raw root-cause count into the number of points that should be
     * deducted, using growing thresholds. Symmetric around zero: a count of `-n`
     * returns the negative of the penalty for `n`.
     */
    private fun penaltyForCount(count: Int): Int {
        if (count == 0) return 0

        val sign = if (count > 0) 1 else -1
        var remaining = if (count > 0) count else -count
        var threshold = config.firstThreshold
        var magnitude = 0

        while (remaining >= threshold) {
            magnitude++
            remaining -= threshold
            threshold += config.thresholdStep
        }

        return sign * magnitude
    }
}

