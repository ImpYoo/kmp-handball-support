package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException

/**
 * Table official team being evaluated for one game.
 *
 * The team is evaluated as a whole. The domain does not capture individual
 * person scores for the timekeeper, scorekeeper, or optional delegate.
 *
 * @property timeKeeper Assignment for the timekeeper position.
 * @property scoreKeeper Assignment for the scorekeeper position.
 * @property delegate Optional assignment for the delegate position.
 */
data class TableOfficialTeam(
    val timeKeeper: RoleAssignment,
    val scoreKeeper: RoleAssignment,
    val delegate: RoleAssignment? = null,
) {
    init {
        requireRole(timeKeeper, OfficialRole.TimeKeeper)
        requireRole(scoreKeeper, OfficialRole.ScoreKeeper)
        delegate?.let { requireRole(it, OfficialRole.Delegate) }

        val seen = mutableSetOf<String>()
        buildList {
            add(timeKeeper.person)
            add(scoreKeeper.person)
            delegate?.person?.let(::add)
        }.forEach { person ->
            if (!seen.add(person.id)) {
                throw DomainException.DuplicatePersonInTeam(person.id)
            }
        }
    }

    /**
     * Distinct members of the evaluated table official team.
     */
    val members: Set<Person> = buildSet {
        add(timeKeeper.person)
        add(scoreKeeper.person)
        delegate?.person?.let(::add)
    }

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
