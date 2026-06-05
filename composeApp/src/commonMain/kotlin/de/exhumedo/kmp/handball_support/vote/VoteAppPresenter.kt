package de.exhumedo.kmp.handball_support.vote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.exhumedo.kmp.handball_support.auth.StoredAuth
import de.exhumedo.kmp.handball_support.auth.TokenStorage
import de.exhumedo.kmp.handball_support.auth.tokenStorage
import de.exhumedo.kmp.handball_support.client.MatchResponseDto
import de.exhumedo.kmp.handball_support.client.PhaseResponseDto
import de.exhumedo.kmp.handball_support.client.VoteApiClient
import de.exhumedo.kmp.handball_support.client.VoteApiException
import de.exhumedo.kmp.handball_support.config.AppConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class VoteAppPresenter(
    private val api: VoteApiClient = VoteApiClient(),
    private val authStorage: TokenStorage = tokenStorage(),
) {
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    var token by mutableStateOf<String?>(null)
    var role by mutableStateOf<String?>(null)

    var phases by mutableStateOf<List<PhaseResponseDto>>(emptyList())
    var selectedPhaseId by mutableStateOf<Int?>(null)
    var matches by mutableStateOf<List<MatchResponseDto>>(emptyList())
    var selectedMatch by mutableStateOf<MatchResponseDto?>(null)

    var evaluatorType by mutableStateOf(VoteEvaluatorType.REFEREE_TEAM)
    var appearance by mutableStateOf(DEFAULT_SCORE)
    var influence by mutableStateOf(DEFAULT_SCORE)
    var teamwork by mutableStateOf(DEFAULT_SCORE)
    var comment by mutableStateOf("")

    var isBusy by mutableStateOf(false)
    var statusMessage by mutableStateOf("Ready")
    var hasExistingVote by mutableStateOf(false)
    var pendingPhaseId by mutableStateOf<Int?>(null)
    var pendingMatchId by mutableStateOf<Int?>(null)

    /**
     * Optional date filter for the phases overview, fed from web GET parameters
     * (`?day=&month=&year=`) and reflected back into the URL.
     */
    var filterDay by mutableStateOf<Int?>(null)
    var filterMonth by mutableStateOf<Int?>(null)
    var filterYear by mutableStateOf<Int?>(null)

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        authStorage.read()?.let {
            token = it.token
            role = it.role
        }
    }

    /** Updates the phases-overview date filter (any component may be null/cleared). */
    fun setDateFilter(day: Int?, month: Int?, year: Int?) {
        filterDay = day?.takeIf { it in 1..31 }
        filterMonth = month?.takeIf { it in 1..12 }
        filterYear = year?.takeIf { it in 1..3000 }
    }

    /**
     * Records that the user is trying to vote on a specific match but is not authenticated yet.
     * The login flow inspects these fields to resume the vote action after a successful sign-in.
     */
    fun markVotePending(phaseId: Int, matchId: Int) {
        pendingPhaseId = phaseId
        pendingMatchId = matchId
    }

    /** Clears any pending vote action — used when the user cancels the login flow. */
    fun cancelPendingVote() {
        pendingPhaseId = null
        pendingMatchId = null
        selectedMatch = null
    }

    suspend fun login() {
        isBusy = true
        statusMessage = "Signing in..."
        try {
            val response = api.login(baseUrl = AppConfig.baseApiUrl, username = username, password = password)
            token = response.accessToken
            role = response.role
            authStorage.save(StoredAuth(token = response.accessToken, role = response.role))
            password = ""
            statusMessage = "Logged in as $username (${response.role})"

            val pendingPhaseIdLocal = pendingPhaseId
            val pendingMatchIdLocal = pendingMatchId
            if (pendingPhaseIdLocal != null && pendingMatchIdLocal != null) {
                if (selectedPhaseId != pendingPhaseIdLocal || matches.none { it.id == pendingMatchIdLocal }) {
                    loadMatches(pendingPhaseIdLocal)
                }
                val pendingMatch = matches.firstOrNull { it.id == pendingMatchIdLocal }
                if (pendingMatch != null) {
                    selectedMatch = pendingMatch
                    evaluatorType = VoteEvaluatorType.REFEREE_TEAM
                    comment = ""
                    resetScoreInputs()
                    hasExistingVote = pendingMatch.hasVoteBy.refereeTeam
                }
            }
            pendingPhaseId = null
            pendingMatchId = null
        } catch (e: Throwable) {
            handleException(e, "Login failed")
        } finally {
            isBusy = false
        }
    }

    fun logout() {
        clearAuthState()
        statusMessage = "Logged out"
    }

    suspend fun loadPhases() {
        isBusy = true
        statusMessage = "Loading phases..."
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
            statusMessage = "Loaded ${phases.size} phases"
        } catch (e: Throwable) {
            phases = emptyList()
            handleException(e, "Failed to load phases")
        } finally {
            isBusy = false
        }
    }

    suspend fun loadMatches(phaseId: Int) {
        isBusy = true
        statusMessage = "Loading matches..."
        try {
            selectedPhaseId = phaseId
            // Newest match days first, but within the same day show the earliest
            // kick-off first (date descending, then time-of-day ascending).
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
            statusMessage = "Loaded ${matches.size} matches of phase $phaseId"
        } catch (e: Throwable) {
            handleException(e, "Failed to load matches")
        } finally {
            isBusy = false
        }
    }

    suspend fun openPhase(phaseId: Int) {
        loadMatches(phaseId)
    }

    fun chooseMatch(match: MatchResponseDto) {
        if (token == null) {
            statusMessage = "Please sign in to open match details"
            pendingPhaseId = selectedPhaseId
            pendingMatchId = match.id
            selectedMatch = match
            return
        }
        selectedMatch = match
        evaluatorType = VoteEvaluatorType.REFEREE_TEAM
        comment = ""
        resetScoreInputs()
        pendingPhaseId = null
        pendingMatchId = null
        hasExistingVote = match.hasVoteBy.refereeTeam
        statusMessage = "Selected match ${match.id}"
    }

    fun updateEvaluatorType(type: VoteEvaluatorType) {
        // Re-selecting the current evaluator is a no-op so an in-progress vote
        // is not reset by tapping the already-selected button.
        if (type == evaluatorType && selectedMatch != null) return
        evaluatorType = type
        val match = selectedMatch
        if (match != null) {
            hasExistingVote = when (type) {
                VoteEvaluatorType.REFEREE_TEAM -> match.hasVoteBy.refereeTeam
                VoteEvaluatorType.DELEGATE -> match.hasVoteBy.delegate
            }
        }
        // Each evaluator type is a separate vote context, so start fresh.
        resetScoreInputs()
        comment = ""
    }

    /** Resets the score selections back to their neutral defaults. */
    private fun resetScoreInputs() {
        appearance = DEFAULT_SCORE
        influence = DEFAULT_SCORE
        teamwork = DEFAULT_SCORE
    }


    suspend fun submitVote() {
        val match = selectedMatch ?: run {
            statusMessage = "Please select a match first"
            return
        }
        val scores = parseScores() ?: return

        isBusy = true
        statusMessage = "Submitting vote..."
        try {
            val payload = VotePayloadFactory.build(
                match = match,
                evaluatorType = evaluatorType,
                appearance = scores.appearance,
                influence = scores.influence,
                teamwork = scores.teamwork,
                comment = comment,
            )
            val response = api.submitVote(AppConfig.baseApiUrl, token = token, payload = payload)
            statusMessage = "Vote saved: id=${response.id}, score=${response.weightedTotalScore}"
            _events.tryEmit(UiEvent.Info("Vote submitted"))
        } catch (e: Throwable) {
            handleException(e, "Failed to submit vote")
        } finally {
            isBusy = false
        }
    }

    private fun clearAuthState() {
        token = null
        role = null
        phases = emptyList()
        matches = emptyList()
        selectedMatch = null
        selectedPhaseId = null
        pendingPhaseId = null
        pendingMatchId = null
        password = ""
        authStorage.clear()
    }

    private fun handleException(e: Throwable, prefix: String) {
        val message = e.toUiMessage(prefix)
        statusMessage = message
        if (e is VoteApiException && e.statusCode == 401) {
            clearAuthState()
            _events.tryEmit(UiEvent.SessionExpired)
        } else {
            _events.tryEmit(UiEvent.Error(message))
        }
    }

    private fun parseScores(): ScoreInputs? {
        val a = appearance.toIntOrNull()
        val i = influence.toIntOrNull()
        val t = teamwork.toIntOrNull()
        if (a == null || i == null || t == null) {
            statusMessage = "Scores must be numbers"
            return null
        }
        if (a !in 1..5 || i !in 1..5 || t !in 1..5) {
            statusMessage = "Scores must be between 1 and 5"
            return null
        }
        return ScoreInputs(a, i, t)
    }

    private data class ScoreInputs(
        val appearance: Int,
        val influence: Int,
        val teamwork: Int,
    )
}

private fun Throwable.toUiMessage(prefix: String): String = when (this) {
    is VoteApiException -> "$prefix (HTTP $statusCode): ${message.orEmpty()}"
    else -> "$prefix: ${message.orEmpty()}"
}

/** Neutral default for the 1..5 score inputs (middle value). */
private const val DEFAULT_SCORE = "3"

/**
 * Converts a match timestamp (seconds or milliseconds) into a [LocalDateTime] in
 * the device's time zone, so matches can be ordered by calendar date and the
 * time of day independently.
 */
@Suppress("DEPRECATION")
private fun matchDateTime(timestamp: Long): LocalDateTime {
    // The API returns seconds in some places and milliseconds in others.
    val epochMillis = if (timestamp < 1_000_000_000_000L) timestamp * 1000L else timestamp
    return Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
}

