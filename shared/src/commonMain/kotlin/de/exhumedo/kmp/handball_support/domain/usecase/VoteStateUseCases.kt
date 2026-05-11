package de.exhumedo.kmp.handball_support.domain.usecase

import de.exhumedo.kmp.handball_support.domain.model.HasVoteBy
import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.VoteByReferees
import de.exhumedo.kmp.handball_support.domain.repository.VoteByRefereesRepository

class EnrichMatchWithVoteStateUseCase(
    private val voteByRefereesRepository: VoteByRefereesRepository,
) {
    operator fun invoke(match: Match): Match {
        val hasRefereeVote = hasRefereeVote(match)
        val hasDelegateVote = hasDelegateVote(match)

        return match.copy(
            hasVoteBy = HasVoteBy(
                refereeTeam = hasRefereeVote,
                delegate = hasDelegateVote,
            ),
        )
    }

    private fun hasRefereeVote(match: Match): Boolean {
        val id = VoteByReferees.refereeVoteId(match.id, match.refereeA.id, match.refereeB.id)
        return voteByRefereesRepository.findById(id) != null
    }

    private fun hasDelegateVote(match: Match): Boolean {
        val delegate = match.delegate ?: return false
        val id = VoteByReferees.delegateVoteId(match.id, delegate.id)
        return voteByRefereesRepository.findById(id) != null
    }
}

