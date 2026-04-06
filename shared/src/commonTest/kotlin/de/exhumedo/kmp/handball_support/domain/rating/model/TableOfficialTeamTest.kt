package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TableOfficialTeamTest {

    @Test
    fun rejectsDuplicatePersons() {
        val sharedPerson = person("t1")

        val exception = assertFailsWith<DomainException.DuplicatePersonInTeam> {
            TableOfficialTeam(
                timeKeeper = RoleAssignment(sharedPerson, OfficialRole.TimeKeeper),
                scoreKeeper = RoleAssignment(sharedPerson, OfficialRole.ScoreKeeper),
                delegate = null,
            )
        }

        assertEquals(sharedPerson.id, exception.personId)
    }

    @Test
    fun includesOptionalDelegateInMembers() {
        val delegate = RoleAssignment(person("d1"), OfficialRole.Delegate)

        val team = TableOfficialTeam(
            timeKeeper = RoleAssignment(person("t1"), OfficialRole.TimeKeeper),
            scoreKeeper = RoleAssignment(person("s1"), OfficialRole.ScoreKeeper),
            delegate = delegate,
        )

        assertEquals(3, team.members.size)
        assertTrue(delegate.person in team.members)
    }
}
