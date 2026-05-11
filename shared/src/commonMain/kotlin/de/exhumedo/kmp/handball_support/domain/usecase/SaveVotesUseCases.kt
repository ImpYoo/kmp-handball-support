package de.exhumedo.kmp.handball_support.domain.usecase

import de.exhumedo.kmp.handball_support.domain.model.CommentTooLongError
import de.exhumedo.kmp.handball_support.domain.model.DomainResult
import de.exhumedo.kmp.handball_support.domain.model.ForbiddenVoterError
import de.exhumedo.kmp.handball_support.domain.model.InvalidRatingError
import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.MissingDelegateError
import de.exhumedo.kmp.handball_support.domain.model.PhaseName
import de.exhumedo.kmp.handball_support.domain.model.VoteAlreadyExistsError
import de.exhumedo.kmp.handball_support.domain.model.VoteByReferees
import de.exhumedo.kmp.handball_support.domain.repository.VoteByRefereesRepository

/**
 * Explicit actor type for a vote submission.
 *
 * - [RefereeTeam] – one of the two assigned referees is submitting the joint referee-team vote.
 *   [voterId] must match [Match.refereeA].id or [Match.refereeB].id.
 * - [Delegate] – the assigned delegate is submitting their separate vote.
 *   [Match.delegate] must be non-null and [voterId] must match [Match.delegate].id.
 */
sealed interface VoteActor {
    val voterId: Int

    data class RefereeTeam(override val voterId: Int) : VoteActor
    data class Delegate(override val voterId: Int) : VoteActor
}

/**
 * Input for saving a vote.  The [actor] field is the single authoritative source of who is voting;
 * all authorization decisions are derived from it together with [match].
 */
data class RefereeVoteInput(
    val match: Match,
    val phaseName: PhaseName,
    val actor: VoteActor,
    val appearanceRating: Int,
    val influenceRating: Int,
    val teamworkRating: Int,
    val comment: String,
)

class SaveVoteByRefereesUseCase(
    private val repository: VoteByRefereesRepository,
) {
    companion object {
        private const val MIN_RATING = 1
        private const val MAX_RATING = 5
        private const val MAX_COMMENT_LENGTH = 2_000
    }

    operator fun invoke(input: RefereeVoteInput): DomainResult<VoteByReferees> {
        val match = input.match

        // --- rating bounds ---
        val hasValidRatings = listOf(input.appearanceRating, input.influenceRating, input.teamworkRating)
            .all { it in MIN_RATING..MAX_RATING }
        if (!hasValidRatings) return DomainResult.Failure(InvalidRatingError)

        // --- comment length ---
        if (input.comment.length > MAX_COMMENT_LENGTH) return DomainResult.Failure(CommentTooLongError)

        // --- actor / authorization ---
        val isDelegateVote: Boolean
        val voteId: String

        when (val actor = input.actor) {
            is VoteActor.RefereeTeam -> {
                val isKnownReferee =
                    actor.voterId == match.refereeA.id || actor.voterId == match.refereeB.id
                if (!isKnownReferee) return DomainResult.Failure(ForbiddenVoterError)
                isDelegateVote = false
                voteId = VoteByReferees.refereeVoteId(match.id, match.refereeA.id, match.refereeB.id)
            }

            is VoteActor.Delegate -> {
                val delegate = match.delegate
                    ?: return DomainResult.Failure(MissingDelegateError)
                if (actor.voterId != delegate.id) return DomainResult.Failure(ForbiddenVoterError)
                isDelegateVote = true
                voteId = VoteByReferees.delegateVoteId(match.id, delegate.id)
            }
        }

        // --- duplicate vote ---
        if (repository.findById(voteId) != null) return DomainResult.Failure(VoteAlreadyExistsError)

        // --- persist ---
        val vote = VoteByReferees(
            id = voteId,
            matchId = match.id,
            phaseId = match.phaseId,
            phaseName = input.phaseName,
            timestamp = match.timestamp,
            homeTeam = match.homeTeam,
            awayTeam = match.awayTeam,
            refereeA = match.refereeA,
            refereeB = match.refereeB,
            delegate = match.delegate,
            timekeeper = match.timekeeper,
            scorekeeper = match.scorekeeper,
            isVoteByDelegate = isDelegateVote,
            appearanceRating = input.appearanceRating,
            influenceRating = input.influenceRating,
            teamworkRating = input.teamworkRating,
            comment = input.comment,
        )
        repository.save(vote)
        return DomainResult.Success(vote)
    }
}
