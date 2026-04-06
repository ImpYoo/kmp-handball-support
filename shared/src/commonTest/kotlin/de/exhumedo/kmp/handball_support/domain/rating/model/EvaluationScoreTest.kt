package de.exhumedo.kmp.handball_support.domain.rating.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EvaluationScoreTest {

    @Test
    fun calculatesWeightedTotalAcrossAllCriteria() {
        val score = EvaluationScore(
            appearance = Score(6),
            influence = Score(7),
            teamwork = Score(8),
        )

        assertEquals(21, score.toScore())
        assertEquals(35, score.toScore(appearanceWeight = 2, influenceWeight = 1, teamworkWeight = 2))
    }

    @Test
    fun rejectsNegativeWeights() {
        val score = EvaluationScore(
            appearance = Score(6),
            influence = Score(7),
            teamwork = Score(8),
        )

        assertFailsWith<IllegalArgumentException> {
            score.toScore(appearanceWeight = -1)
        }
    }
}
