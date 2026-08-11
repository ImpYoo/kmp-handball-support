package de.exhumedo.kmp.handball_support.referee_coaching.domain.usecase

import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion
import de.exhumedo.kmp.handball_support.referee_coaching.domain.scoring.CriterionScoringService

/**
 * Application entry point for adjusting a criterion's score by selecting or
 * deselecting a root cause. Delegates the scoring rules to
 * [CriterionScoringService] and exposes a single, intent-revealing operation
 * for the presentation layer.
 */
class AdjustCriterionScoreUseCase(
    private val scoringService: CriterionScoringService = CriterionScoringService(),
) {

    /** Direction of a root-cause adjustment. */
    enum class Adjustment { SELECT, DESELECT }

    operator fun invoke(
        criterion: Criterion,
        groupId: String,
        rootCauseId: String,
        adjustment: Adjustment,
    ): Criterion = when (adjustment) {
        Adjustment.SELECT -> scoringService.incrementRootCause(criterion, groupId, rootCauseId)
        Adjustment.DESELECT -> scoringService.decrementRootCause(criterion, groupId, rootCauseId)
    }
}

