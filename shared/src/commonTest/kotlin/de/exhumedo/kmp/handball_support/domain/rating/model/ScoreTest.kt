package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ScoreTest {

    @Test
    fun rejectsValuesOutsideFederationRange() {
        assertFailsWith<DomainException.InvalidScoreRange> {
            Score(0)
        }

        assertFailsWith<DomainException.InvalidScoreRange> {
            Score(11)
        }
    }
}
