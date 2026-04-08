package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EvaluatorTest {

    @Test
    fun delegateEvaluatorRequiresDelegateRole() {
        val exception = assertFailsWith<DomainException.InvalidRoleForPosition> {
            Evaluator.Delegate(
                RoleAssignment(
                    person = person("d1"),
                    role = OfficialRole.ScoreKeeper,
                ),
            )
        }

        assertEquals("Delegate", exception.expectedRole)
        assertEquals("ScoreKeeper", exception.actualRole)
    }
}
