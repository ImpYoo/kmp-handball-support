package de.exhumedo.kmp.handball_support.application

import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.Phase
import de.exhumedo.kmp.handball_support.domain.repository.PhaseRepository
import de.exhumedo.kmp.handball_support.domain.repository.VoteByRefereesRepository
import de.exhumedo.kmp.handball_support.domain.usecase.EnrichMatchWithVoteStateUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetMatchOfPhaseUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetMatchesOfPhaseByTimestampUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetPhasesWithMatchesByTimestampUseCase

/**
 * Orchestrates the shared-domain read use cases for phases and matches.
 *
 * @param phaseRepository Source of phase and match data (mock or real).
 * @param voteRepository  Used to enrich each match with its current vote state.
 */
class MatchApplicationService(
    private val phaseRepository: PhaseRepository,
    private val voteRepository: VoteByRefereesRepository,
) {
    private val getPhasesWithMatches = GetPhasesWithMatchesByTimestampUseCase()
    private val getMatchesOfPhase    = GetMatchesOfPhaseByTimestampUseCase()
    private val getMatchOfPhase      = GetMatchOfPhaseUseCase()
    private val enrichWithVoteState  = EnrichMatchWithVoteStateUseCase(voteRepository)

    fun getPhasesFromTimestamp(fromTimestamp: Long): List<Phase> {
        val phases = phaseRepository.getAllPhases()
        return getPhasesWithMatches(phases, fromTimestamp)
    }

    fun getMatchesOfPhaseFromTimestamp(phaseId: Int, fromTimestamp: Long): List<Match> {
        val phases = phaseRepository.getAllPhases()
        return getMatchesOfPhase(phaseId, phases, fromTimestamp)
            .map { enrichWithVoteState(it) }
    }

    fun getMatch(phaseId: Int, matchId: Int): Match? {
        val phases = phaseRepository.getAllPhases()
        val match = getMatchOfPhase(phaseId, matchId, phases) ?: return null
        return enrichWithVoteState(match)
    }
}
