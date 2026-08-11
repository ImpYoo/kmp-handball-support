package de.exhumedo.kmp.handball_support.referee_coaching.domain

import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.referee_coaching.domain.usecase.AdjustCriterionScoreUseCase
import de.exhumedo.kmp.handball_support.referee_coaching.domain.usecase.AdjustCriterionScoreUseCase.Adjustment
import de.exhumedo.kmp.handball_support.referee_coaching.domain.usecase.LoadCriteriaUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class RefereeCoachingUseCaseTest {

    private val loadCriteria = LoadCriteriaUseCase(DefaultCriterionCatalog())
    private val adjustScore = AdjustCriterionScoreUseCase()

    @Test
    fun loadCriteriaReturnsCatalog() {
        val criteria = loadCriteria()
        assertEquals(DefaultCriterionCatalog().loadCriteria(), criteria)
    }

    @Test
    fun selectingARootCauseThreeTimesDeductsOnePoint() {
        val criterion = loadCriteria().first()
        val group = criterion.defectGroups.first()
        val cause = group.rootCauses.first()

        var result = criterion
        repeat(3) {
            result = adjustScore(result, group.id, cause.id, Adjustment.SELECT)
        }

        assertEquals(criterion.score - 1, result.score)
    }

    @Test
    fun deselectingRestoresThePoint() {
        val criterion = loadCriteria().first()
        val group = criterion.defectGroups.first()
        val cause = group.rootCauses.first()

        var result = criterion
        repeat(3) { result = adjustScore(result, group.id, cause.id, Adjustment.SELECT) }
        result = adjustScore(result, group.id, cause.id, Adjustment.DESELECT)

        assertEquals(criterion.score, result.score)
    }

    @Test
    fun deselectingBelowZeroIsSymmetricBonus() {
        val criterion = loadCriteria().first()
        val group = criterion.defectGroups.first()
        val cause = group.rootCauses.first()

        // Three deselects mirror three selects: count -3 grants +1 point.
        var result = criterion
        repeat(3) { result = adjustScore(result, group.id, cause.id, Adjustment.DESELECT) }

        assertEquals(criterion.score + 1, result.score)
        assertEquals(-3, result.defectGroups.first().rootCauses.first().count)
    }
}


