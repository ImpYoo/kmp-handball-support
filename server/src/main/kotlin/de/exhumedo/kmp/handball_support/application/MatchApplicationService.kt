package de.exhumedo.kmp.handball_support.application

import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.Phase
import de.exhumedo.kmp.handball_support.domain.repository.PhaseRepository
import de.exhumedo.kmp.handball_support.domain.repository.VoteByRefereesRepository
import de.exhumedo.kmp.handball_support.domain.usecase.EnrichMatchWithVoteStateUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetMatchOfPhaseUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetMatchesOfPhaseByTimestampUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetPhasesWithMatchesByTimestampUseCase
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Orchestrates the shared-domain read use cases for phases and matches.
 *
 * All list endpoints are scoped to a rolling 3-day window:
 * from the start of (today − 2 days) up to (but not including) the start of tomorrow,
 * all in UTC. This ensures only games from today, yesterday, or the day before yesterday
 * are returned.
 *
 * @param phaseRepository Source of phase and match data (mock or real).
 * @param voteRepository  Used to enrich each match with its current vote state.
 * @param clock           Overridable UTC clock; defaults to [Clock.systemUTC].
 */
class MatchApplicationService(
    private val phaseRepository: PhaseRepository,
    private val voteRepository: VoteByRefereesRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val getPhasesWithMatches = GetPhasesWithMatchesByTimestampUseCase()
    private val getMatchesOfPhase    = GetMatchesOfPhaseByTimestampUseCase()
    private val getMatchOfPhase      = GetMatchOfPhaseUseCase()
    private val enrichWithVoteState  = EnrichMatchWithVoteStateUseCase(voteRepository)

    fun getPhases(): List<Phase> {
        val (from, to) = window()
        val phases = phaseRepository.getAllPhases()
        return getPhasesWithMatches(phases, from, to)
    }

    fun getMatchesOfPhase(phaseId: Int): List<Match> {
        val (from, to) = window()
        val phases = phaseRepository.getAllPhases()
        return getMatchesOfPhase(phaseId, phases, from, to)
            .map { enrichWithVoteState(it) }
    }

    fun getMatch(phaseId: Int, matchId: Int): Match? {
        val phases = phaseRepository.getAllPhases()
        val match = getMatchOfPhase(phaseId, matchId, phases) ?: return null
        return enrichWithVoteState(match)
    }

    /** Returns [fromTimestamp, toTimestamp) covering today and the 2 preceding days (UTC). */
    private fun window(): Pair<Long, Long> {
        val today = LocalDate.now(clock.zone).atStartOfDay(ZoneOffset.UTC)
        val from  = today.minusDays(2).toEpochSecond()
        val to    = today.plusDays(1).toEpochSecond()   // exclusive upper bound (start of tomorrow)
        return from to to
    }
}
