package de.exhumedo.kmp.handball_support.vote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.exhumedo.kmp.handball_support.client.MatchResponseDto
import de.exhumedo.kmp.handball_support.client.PhaseResponseDto
import de.exhumedo.kmp.handball_support.client.VoteApiClient
import de.exhumedo.kmp.handball_support.config.AppConfig
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Holds the phases/matches list state and the optional date filter.
 * Responsible for loading phases and matches from the API.
 */
class MatchListPresenter(
    private val api: VoteApiClient = VoteApiClient(),
) {
    var phases by mutableStateOf<List<PhaseResponseDto>>(emptyList())
    var selectedPhaseId by mutableStateOf<Int?>(null)
    var matches by mutableStateOf<List<MatchResponseDto>>(emptyList())
    var selectedMatch by mutableStateOf<MatchResponseDto?>(null)

    var filterDay by mutableStateOf<Int?>(null)
    var filterMonth by mutableStateOf<Int?>(null)
    var filterYear by mutableStateOf<Int?>(null)

    var pendingPhaseId by mutableStateOf<Int?>(null)
    var pendingMatchId by mutableStateOf<Int?>(null)

    /** Updates the phases-overview date filter (any component may be null/cleared). */
    fun setDateFilter(day: Int?, month: Int?, year: Int?) {
        filterDay = day?.takeIf { it in 1..31 }
        filterMonth = month?.takeIf { it in 1..12 }
        filterYear = year?.takeIf { it in 1..3000 }
    }

    suspend fun loadPhases(onError: (Throwable, String) -> Unit, onBusyChange: (Boolean, String) -> Unit) {
        onBusyChange(true, "Loading phases...")
        try {
            phases = api.getPhases(
                baseUrl = AppConfig.baseApiUrl,
                day = filterDay,
                month = filterMonth,
                year = filterYear,
            )
            selectedPhaseId = null
            matches = emptyList()
            selectedMatch = null
            onBusyChange(false, "Loaded ${phases.size} phases")
        } catch (e: Throwable) {
            phases = emptyList()
            onError(e, "Failed to load phases")
        }
    }

    suspend fun loadMatches(phaseId: Int, onError: (Throwable, String) -> Unit, onBusyChange: (Boolean, String) -> Unit) {
        onBusyChange(true, "Loading matches...")
        try {
            selectedPhaseId = phaseId
            matches = api.getMatches(
                baseUrl = AppConfig.baseApiUrl,
                phaseId = phaseId,
                day = filterDay,
                month = filterMonth,
                year = filterYear,
            ).sortedWith(
                compareByDescending<MatchResponseDto> { matchDateTime(it.timestamp).date }
                    .thenBy { matchDateTime(it.timestamp).time },
            )
            selectedMatch = null
            onBusyChange(false, "Loaded ${matches.size} matches of phase $phaseId")
        } catch (e: Throwable) {
            onError(e, "Failed to load matches")
        }
    }

    fun markVotePending(phaseId: Int, matchId: Int) {
        pendingPhaseId = phaseId
        pendingMatchId = matchId
    }

    fun cancelPendingVote() {
        pendingPhaseId = null
        pendingMatchId = null
        selectedMatch = null
    }

    fun resetListState() {
        phases = emptyList()
        matches = emptyList()
        selectedMatch = null
        selectedPhaseId = null
        pendingPhaseId = null
        pendingMatchId = null
    }
}

@Suppress("DEPRECATION")
internal fun matchDateTime(timestamp: Long): LocalDateTime {
    val epochMillis = if (timestamp < 1_000_000_000_000L) timestamp * 1000L else timestamp
    return Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
}