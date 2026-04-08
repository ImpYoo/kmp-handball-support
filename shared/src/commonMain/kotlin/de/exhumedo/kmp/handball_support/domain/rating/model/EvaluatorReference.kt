package de.exhumedo.kmp.handball_support.domain.rating.model

/**
 * Query-side reference to an evaluator without reconstructing the full aggregate.
 *
 * This supports domain-level queries without leaking one evaluator subtype into
 * repository interfaces.
 */
sealed class EvaluatorReference {
    /**
     * Reference to a referee-team evaluator by both referee person identifiers.
     */
    data class RefereeTeam(
        val firstRefereeId: String,
        val secondRefereeId: String,
    ) : EvaluatorReference()

    /**
     * Reference to a delegate evaluator by delegate person identifier.
     */
    data class Delegate(
        val delegateId: String,
    ) : EvaluatorReference()
}
