package de.exhumedo.kmp.handball_support.domain.rating.exception

/**
 * Base type for all domain-specific failures.
 *
 * The domain model uses a sealed exception hierarchy so callers can handle
 * invariant violations explicitly without depending on framework-specific
 * exception types.
 */
sealed class DomainException(message: String) : Exception(message) {

    /**
     * Indicates that a score value falls outside the allowed inclusive range.
     *
     * @property value The invalid score value.
     * @property min The minimum allowed score.
     * @property max The maximum allowed score.
     */
    data class InvalidScoreRange(
        val value: Int,
        val min: Int,
        val max: Int,
    ) : DomainException("Score value $value is out of valid range [$min, $max]")

    /**
     * Indicates that the same person is assigned more than once within one game context.
     *
     * @property personId The duplicated person identifier.
     */
    data class DuplicatePersonInTeam(
        val personId: String,
    ) : DomainException("Person with id '$personId' is assigned to multiple roles in the same context")

    /**
     * Indicates that a role assignment does not match the required position.
     *
     * @property expectedRole The role that was required.
     * @property actualRole The role that was actually supplied.
     */
    data class InvalidRoleForPosition(
        val expectedRole: String,
        val actualRole: String,
    ) : DomainException("Expected role $expectedRole but got $actualRole")

}
