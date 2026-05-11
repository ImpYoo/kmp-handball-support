package de.exhumedo.kmp.handball_support.domain

import de.exhumedo.kmp.handball_support.domain.model.MatchDay
import de.exhumedo.kmp.handball_support.domain.model.Phase
import de.exhumedo.kmp.handball_support.domain.usecase.GetMatchOfPhaseUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetMatchesOfPhaseByTimestampUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.GetPhasesWithMatchesByTimestampUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadMatchesUseCasesTest {

    private val match1 = matchWithDelegate(matchId = 1).copy(timestamp = 1_000L, phaseId = 10)
    private val match2 = matchWithDelegate(matchId = 2).copy(timestamp = 2_000L, phaseId = 10)
    private val match3 = matchWithDelegate(matchId = 3).copy(timestamp = 3_000L, phaseId = 20)

    private val phase10 = Phase(
        phaseId = 10,
        tournamentId = 1,
        seasonId = 1,
        name = "Bundesliga",
        shortName = "BL",
        matchDays = listOf(
            MatchDay(id = 1, matches = listOf(match1, match2)),
        ),
    )
    private val phase20 = Phase(
        phaseId = 20,
        tournamentId = 1,
        seasonId = 1,
        name = "Cup",
        shortName = "CUP",
        matchDays = listOf(
            MatchDay(id = 2, matches = listOf(match3)),
        ),
    )
    private val allPhases = listOf(phase10, phase20)

    // ── GetPhasesWithMatchesByTimestampUseCase ────────────────────────────────

    @Test
    fun getPhasesReturnsAllPhasesWhenFromTimestampIsZero() {
        val result = GetPhasesWithMatchesByTimestampUseCase()(allPhases, 0L)
        assertEquals(2, result.size)
    }

    @Test
    fun getPhasesFiltersOutMatchesBelowTimestamp() {
        val result = GetPhasesWithMatchesByTimestampUseCase()(allPhases, 2_000L)
        val phase = result.first { it.phaseId == 10 }
        assertEquals(1, phase.matchDays.first().matches.size)
        assertEquals(match2, phase.matchDays.first().matches.first())
    }

    @Test
    fun getPhasesExcludesPhasesWithNoRemainingMatches() {
        // timestamp higher than all match1+match2 but lower than match3 → phase10 excluded, phase20 kept
        val result = GetPhasesWithMatchesByTimestampUseCase()(allPhases, 2_500L)
        assertEquals(1, result.size)
        assertEquals(20, result.first().phaseId)
    }

    @Test
    fun getPhasesReturnsEmptyListWhenAllMatchesFiltered() {
        val result = GetPhasesWithMatchesByTimestampUseCase()(allPhases, 99_999L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getPhasesReturnsEmptyListForEmptyInput() {
        val result = GetPhasesWithMatchesByTimestampUseCase()(emptyList(), 0L)
        assertTrue(result.isEmpty())
    }

    // ── GetMatchesOfPhaseByTimestampUseCase ──────────────────────────────────

    @Test
    fun getMatchesOfPhaseReturnsAllMatchesFromTimestampZero() {
        val result = GetMatchesOfPhaseByTimestampUseCase()(phaseId = 10, phases = allPhases, fromTimestamp = 0L)
        assertEquals(2, result.size)
    }

    @Test
    fun getMatchesOfPhaseFiltersCorrectly() {
        val result = GetMatchesOfPhaseByTimestampUseCase()(phaseId = 10, phases = allPhases, fromTimestamp = 2_000L)
        assertEquals(1, result.size)
        assertEquals(match2, result.first())
    }

    @Test
    fun getMatchesOfPhaseReturnsEmptyListForUnknownPhase() {
        val result = GetMatchesOfPhaseByTimestampUseCase()(phaseId = 999, phases = allPhases, fromTimestamp = 0L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getMatchesOfPhaseReturnsEmptyListForEmptyPhaseList() {
        val result = GetMatchesOfPhaseByTimestampUseCase()(phaseId = 10, phases = emptyList(), fromTimestamp = 0L)
        assertTrue(result.isEmpty())
    }

    // ── GetMatchOfPhaseUseCase ───────────────────────────────────────────────

    @Test
    fun getMatchOfPhaseReturnsMatchWhenFound() {
        val result = GetMatchOfPhaseUseCase()(phaseId = 10, matchId = 2, phases = allPhases)
        assertEquals(match2, result)
    }

    @Test
    fun getMatchOfPhaseReturnsNullForUnknownMatchId() {
        val result = GetMatchOfPhaseUseCase()(phaseId = 10, matchId = 999, phases = allPhases)
        assertNull(result)
    }

    @Test
    fun getMatchOfPhaseReturnsNullForUnknownPhaseId() {
        val result = GetMatchOfPhaseUseCase()(phaseId = 999, matchId = 1, phases = allPhases)
        assertNull(result)
    }

    @Test
    fun getMatchOfPhaseReturnsNullForEmptyPhaseList() {
        val result = GetMatchOfPhaseUseCase()(phaseId = 10, matchId = 1, phases = emptyList())
        assertNull(result)
    }
}

