package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException

/**
 * Party that evaluates one table official team for one game.
 *
 * A game can be evaluated once by the referee team and once by the delegate.
 */
sealed class Evaluator {

    /**
     * Stable evaluator kind used for uniqueness checks and persistence.
     */
    abstract val type: EvaluatorType

    /**
     * All persons represented by this evaluator.
     */
    abstract val persons: Set<Person>

    /**
     * Referee team acting as one collective evaluator.
     */
    data class RefereeTeam(
        val refereePair: RefereePair,
    ) : Evaluator() {
        override val type: EvaluatorType = EvaluatorType.REFEREE_TEAM
        override val persons: Set<Person> = refereePair.persons
    }

    /**
     * Delegate acting as an individual evaluator.
     */
    data class Delegate(
        val assignment: RoleAssignment,
    ) : Evaluator() {
        init {
            if (assignment.role != OfficialRole.Delegate) {
                throw DomainException.InvalidRoleForPosition(
                    expectedRole = OfficialRole.Delegate.displayName,
                    actualRole = assignment.role.displayName,
                )
            }
        }

        override val type: EvaluatorType = EvaluatorType.DELEGATE
        override val persons: Set<Person> = setOf(assignment.person)
    }
}

/**
 * Supported evaluator kinds.
 */
enum class EvaluatorType {
    /**
     * Collective evaluation submitted by the referee team.
     */
    REFEREE_TEAM,

    /**
     * Individual evaluation submitted by the delegate.
     */
    DELEGATE,
}
