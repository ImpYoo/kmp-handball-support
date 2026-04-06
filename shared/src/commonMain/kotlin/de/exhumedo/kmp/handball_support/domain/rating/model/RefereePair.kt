package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException

/**
 * Referee pair assigned to a game.
 *
 * The pair acts as one collective voter: both referees submit a single shared
 * evaluation of the table official team for the game.
 *
 * @property firstReferee Assignment for the first referee position.
 * @property secondReferee Assignment for the second referee position.
 */
data class RefereePair(
    val firstReferee: RoleAssignment,
    val secondReferee: RoleAssignment,
) {
    init {
        requireRole(
            assignment = firstReferee,
            expected = OfficialRole.FirstReferee,
        )
        requireRole(
            assignment = secondReferee,
            expected = OfficialRole.SecondReferee,
        )

        if (firstReferee.person.id == secondReferee.person.id) {
            throw DomainException.DuplicatePersonInTeam(firstReferee.person.id)
        }
    }

    /**
     * Distinct referee persons participating in the collective vote.
     */
    val persons: Set<Person> = setOf(firstReferee.person, secondReferee.person)

    private fun requireRole(
        assignment: RoleAssignment,
        expected: OfficialRole,
    ) {
        if (assignment.role != expected) {
            throw DomainException.InvalidRoleForPosition(
                expectedRole = expected.displayName,
                actualRole = assignment.role.displayName,
            )
        }
    }
}
