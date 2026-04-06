package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RefereePairTest {

    @Test
    fun requiresContextualRoles() {
        val exception = assertFailsWith<DomainException.InvalidRoleForPosition> {
            RefereePair(
                firstReferee = RoleAssignment(person("r1"), OfficialRole.SecondReferee),
                secondReferee = RoleAssignment(person("r2"), OfficialRole.SecondReferee),
            )
        }

        assertEquals("FirstReferee", exception.expectedRole)
        assertEquals("SecondReferee", exception.actualRole)
    }
}
