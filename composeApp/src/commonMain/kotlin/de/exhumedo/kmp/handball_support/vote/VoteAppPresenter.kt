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

/**
 * Thin facade over [AuthPresenter], [MatchListPresenter], and [VoteFormPresenter].
 *
 * Exists for backward compatibility with screens that expect a single presenter.
 * New code should reference the sub-presenters directly.
 */
class VoteAppPresenter(
    private val api: VoteApiClient = VoteApiClient(),
    private val authStorage: TokenStorage = tokenStorage(),
) {
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val auth = AuthPresenter(api, authStorage, _events)
    val matchList = MatchListPresenter(api)
    val voteForm = VoteFormPresenter(api)

    // --- Auth state (delegated) ---
    var username by auth::username
    var password by auth::password
    var token by auth::token
    var role by auth::role
    var isBusy by auth::isBusy
    var statusMessage by auth::statusMessage

    // --- Match list state (delegated) ---
    var phases by matchList::phases
    var selectedPhaseId by matchList::selectedPhaseId
    var matches by matchList::matches
    var selectedMatch by matchList::selectedMatch
    var filterDay by matchList::filterDay
    var filterMonth by matchList::filterMonth
    var filterYear by matchList::filterYear
    var pendingPhaseId by matchList::pendingPhaseId
    var pendingMatchId by matchList::pendingMatchId

    // --- Vote form state (delegated) ---
    var evaluatorType by voteForm::evaluatorType
    var appearance by voteForm::appearance
    var influence by voteForm::influence
    var teamwork by voteForm::teamwork
    var comment by voteForm::comment
    var hasExistingVote by voteForm::hasExistingVote

    init {
        auth.eventsFlow
    }

    // --- Auth actions ---
    suspend fun login() {
        auth.login(
            onPendingVoteResume = { phaseId, matchId ->
                if (selectedPhaseId != phaseId || matches.none { it.id == matchId }) {
                    loadMatches(phaseId)
                }
                val pendingMatch = matches.firstOrNull { it.id == matchId }
                if (pendingMatch != null) {
                    selectedMatch = pendingMatch
                    voteForm.chooseMatch(pendingMatch)
                }
            },
            pendingPhaseId = pendingPhaseId,
            pendingMatchId = pendingMatchId,
        )
        pendingPhaseId = null
        pendingMatchId = null
    }

    fun logout() {
        auth.logout()
        matchList.resetListState()
    }

    // --- Match list actions ---
    fun setDateFilter(day: Int?, month: Int?, year: Int?) =
        matchList.setDateFilter(day, month, year)

    suspend fun loadPhases() =
        matchList.loadPhases(onError = ::handleException, onBusyChange = ::setBusy)

    suspend fun loadMatches(phaseId: Int) =
        matchList.loadMatches(phaseId, onError = ::handleException, onBusyChange = ::setBusy)

    suspend fun openPhase(phaseId: Int) = loadMatches(phaseId)

    fun chooseMatch(match: MatchResponseDto) {
        if (token == null) {
            statusMessage = "Please sign in to open match details"
            matchList.markVotePending(selectedPhaseId ?: 0, match.id)
            selectedMatch = match
            return
        }
        selectedMatch = match
        voteForm.chooseMatch(match)
        matchList.pendingPhaseId = null
        matchList.pendingMatchId = null
        statusMessage = "Selected match ${match.id}"
    }

    fun markVotePending(phaseId: Int, matchId: Int) =
        matchList.markVotePending(phaseId, matchId)

    fun cancelPendingVote() = matchList.cancelPendingVote()

    // --- Vote form actions ---
    fun updateEvaluatorType(type: VoteEvaluatorType) =
        voteForm.updateEvaluatorType(type, selectedMatch)

    suspend fun submitVote() {
        val match = selectedMatch ?: run {
            statusMessage = "Please select a match first"
            return
        }
        voteForm.submitVote(
            match = match,
            token = token,
            onBusyChange = ::setBusy,
            onSuccess = { _, _ -> _events.tryEmit(UiEvent.Info("Vote submitted")) },
            onError = ::handleException,
        )
    }

    // --- Shared helpers ---
    private fun setBusy(busy: Boolean, message: String) {
        isBusy = busy
        statusMessage = message
    }

    private fun handleException(e: Throwable, prefix: String) {
        auth.handleError(e, prefix)
        if (e is VoteApiException && e.statusCode == 401) {
            matchList.resetListState()
        }
    }
}