package de.exhumedo.kmp.handball_support.application

import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.Phase
import de.exhumedo.kmp.handball_support.domain.repository.PhaseRepository
import de.exhumedo.kmp.handball_support.domain.repository.VoteByRefereesRepository
import de.exhumedo.kmp.handball_support.domain.usecase.EnrichMatchWithVoteStateUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetMatchOfPhaseUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetMatchesOfPhaseByTimestampUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetPhasesWithMatchesByTimestampUseCase
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Orchestrates the shared-domain read use cases for phases and matches.
 *
 * The primary API uses a server-computed 3-day window (yesterday-1 → tomorrow) so callers
 * do not need to supply timestamps. The window covers:
 *   - matches 2 days ago (e.g. Sunday games visible on Tuesday)
 *   - today
 *   - tomorrow (pre-announced games)
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

    /** Returns phases containing matches within the server-computed 3-day window. */
    fun getPhases(): List<Phase> {
        val phases = phaseRepository.getAllPhases()
        val (from, to) = window()
        return getPhasesWithMatches(phases, from, to)
    }

    /** Returns matches for [phaseId] within the server-computed 3-day window. */
    fun getMatchesOfPhase(phaseId: Int): List<Match> {
        val phases = phaseRepository.getAllPhases()
        val (from, to) = window()
        return getMatchesOfPhase(phaseId, phases, from, to)
            .map { enrichWithVoteState(it) }
    }

    /** Returns a single match by [phaseId] + [matchId], regardless of the time window. */
    fun getMatch(phaseId: Int, matchId: Int): Match? {
        val phases = phaseRepository.getAllPhases()
        val match = getMatchOfPhase(phaseId, matchId, phases) ?: return null
        return enrichWithVoteState(match)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * [from, to) window in UTC epoch seconds:
     *   from = midnight 2 days ago
     *   to   = midnight tomorrow
     */
    private fun window(): Pair<Long, Long> {
        val today = LocalDate.now(ZoneOffset.UTC)
        val from  = today.minusDays(2).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        val to    = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        return from to to
    }
}
