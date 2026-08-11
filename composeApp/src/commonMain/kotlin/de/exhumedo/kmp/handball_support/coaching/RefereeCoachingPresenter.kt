package de.exhumedo.kmp.handball_support.coaching

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CriterionCategory
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.ScoringConfig
import de.exhumedo.kmp.handball_support.referee_coaching.domain.usecase.AdjustCriterionScoreUseCase
import de.exhumedo.kmp.handball_support.referee_coaching.domain.usecase.AdjustCriterionScoreUseCase.Adjustment
import de.exhumedo.kmp.handball_support.referee_coaching.domain.usecase.LoadCriteriaUseCase

/**
 * UI state holder for the referee-coaching screen (HVNB "Beobachterbericht").
 *
 * Pure presentation glue: it holds the working list of [Criterion] in Compose
 * state and delegates every scoring decision to the shared domain use cases, so
 * the deduction rules live in one place and stay testable.
 */
class RefereeCoachingPresenter(
    private val loadCriteria: LoadCriteriaUseCase = LoadCriteriaUseCase(DefaultCriterionCatalog()),
    private val adjustScore: AdjustCriterionScoreUseCase = AdjustCriterionScoreUseCase(),
) {
    /** The live evaluation: each criterion carries its current score and counts. */
    var criteria by mutableStateOf(loadCriteria())
        private set

    /** Criteria belonging to section A — "Spielregeln". */
    val rulesOfTheGame: List<Criterion>
        get() = criteria.filter { it.category == CriterionCategory.RULES_OF_THE_GAME }

    /** Criteria belonging to section B — "Persönlicher Eindruck". */
    val personalImpression: List<Criterion>
        get() = criteria.filter { it.category == CriterionCategory.PERSONAL_IMPRESSION }

    /** Sum of all criterion scores — the sheet's "Gesamtpunktzahl". */
    val totalScore: Int
        get() = criteria.sumOf { it.score }

    /** Maximum achievable total, used to render the score out of its ceiling. */
    val maxTotalScore: Int
        get() = criteria.size * ScoringConfig.MAX_SCORE

    /** Number of criteria whose score differs from the default (deduction or bonus). */
    val adjustedCriteriaCount: Int
        get() = criteria.count { it.score != ScoringConfig.DEFAULT_SCORE }

    /** Selects (increments) a root cause and recomputes the owning criterion's score. */
    fun select(criterionId: String, groupId: String, rootCauseId: String) =
        adjust(criterionId, groupId, rootCauseId, Adjustment.SELECT)

    /** Deselects (decrements) a root cause and recomputes the owning criterion's score. */
    fun deselect(criterionId: String, groupId: String, rootCauseId: String) =
        adjust(criterionId, groupId, rootCauseId, Adjustment.DESELECT)

    /** Resets the whole evaluation back to a clean template. */
    fun reset() {
        criteria = loadCriteria()
    }

    private fun adjust(
        criterionId: String,
        groupId: String,
        rootCauseId: String,
        adjustment: Adjustment,
    ) {
        criteria = criteria.map { criterion ->
            if (criterion.id != criterionId) {
                criterion
            } else {
                adjustScore(criterion, groupId, rootCauseId, adjustment)
            }
        }
    }
}


