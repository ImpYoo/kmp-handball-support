package de.exhumedo.kmp.handball_support.coaching

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.exhumedo.kmp.handball_support.client.CoachingApiClient
import de.exhumedo.kmp.handball_support.client.CoachingGameDto
import de.exhumedo.kmp.handball_support.client.CoachingPersonDto
import de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto
import de.exhumedo.kmp.handball_support.client.CreateCoachingEvaluationRequestDto
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.ScoringConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Cross-cutting coordinator for the referee coaching session.
 *
 * - Keeps the locally persisted session state authoritative (offline backup).
 * - Sends debounced snapshots to the backend when a token is available and the
 *   base URL is reachable.
 * - Only uploads when the coaching payload actually changes (criteria counts,
 *   history entries, setup fields).
 * - Surfaces the last server-side report so the UI can render it without
 *   recomputing presentation in the client.
 */
class CoachingSessionSync(
    private val apiClient: CoachingApiClient = CoachingApiClient(),
) {
    /** Latest server-side evaluation id; null until first successful upload. */
    var evaluationId by mutableStateOf<String?>(null)

    /** Latest server-side report; null when no upload has succeeded yet. */
    var report by mutableStateOf<CoachingReportResponseDto?>(null)
        private set

    /** Human-readable status of the last sync attempt. */
    var status by mutableStateOf<SyncStatus>(SyncStatus.Idle)
        private set

    private var pendingJob: Job? = null

    /**
     * Start reactive auto-save.
     *
     * [snapshotFlow] must produce a fresh payload on every relevant state change.
     * This method adds debounce + distinctUntilChanged so identical snapshots
     * never trigger a network request.
     */
    fun startAutoSave(
        scope: CoroutineScope,
        baseUrl: String,
        token: String?,
        snapshotFlow: Flow<CreateCoachingEvaluationRequestDto>,
    ) {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            @OptIn(FlowPreview::class)
            snapshotFlow
                .debounce(1.seconds)
                .distinctUntilChanged { old, new -> old.isContentEqualTo(new) }
                .collectLatest { request ->
                    if (token.isNullOrBlank()) {
                        status = SyncStatus.Offline("No API configured or not signed in")
                        return@collectLatest
                    }
                    if (request.evaluatorUsername.isBlank()) {
                        status = SyncStatus.Offline("Evaluator username is required")
                        return@collectLatest
                    }
                    if (baseUrl.isBlank()) {
                        status = SyncStatus.Offline("No API configured")
                        return@collectLatest
                    }
                    status = SyncStatus.Syncing
                    status = try {
                        val response = apiClient.saveEvaluation(
                            baseUrl = baseUrl,
                            token = token,
                            evaluationId = evaluationId,
                            payload = request,
                        )
                        evaluationId = response.id
                        val freshReport = apiClient.getReport(baseUrl, token, response.id)
                        report = freshReport
                        SyncStatus.Success("Saved at ${response.updatedAt}")
                    } catch (e: Exception) {
                        SyncStatus.Error(e.message ?: "Unknown sync error")
                    }
                }
        }
    }

    fun stopAutoSave() {
        pendingJob?.cancel()
        pendingJob = null
    }

    private fun CreateCoachingEvaluationRequestDto.isContentEqualTo(other: CreateCoachingEvaluationRequestDto): Boolean {
        return game == other.game &&
            evaluatorUsername == other.evaluatorUsername &&
            firstReferee == other.firstReferee &&
            secondReferee == other.secondReferee &&
            rootCauseCounts == other.rootCauseCounts &&
            comment == other.comment &&
            history == other.history
    }

    /**
     * Build a snapshot request from present observable state.
     *
     * This is a pure helper so callers can wire it into a [snapshotFlow].
     */
    fun buildRequest(
        gameId: String,
        matchDate: String,
        homeTeam: String,
        awayTeam: String,
        evaluatorUsername: String,
        firstRefereeName: String,
        secondRefereeName: String,
        criteria: List<Criterion>,
        comment: String,
        history: List<CoachingHistoryEntry>,
    ): CreateCoachingEvaluationRequestDto {
        val counts = criteria.associate { criterion ->
            criterion.id to criterion.defectGroups.associate { group ->
                group.id to group.rootCauses
                    .filter { it.count != 0 }
                    .associate { it.id to it.count }
            }
        }
        return CreateCoachingEvaluationRequestDto(
            game = CoachingGameDto(
                gameId = gameId,
                matchDate = matchDate,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
            ),
            evaluatorUsername = evaluatorUsername,
            firstReferee = CoachingPersonDto(
                personId = "referee-1",
                firstName = firstRefereeName,
                lastName = "",
            ),
            secondReferee = CoachingPersonDto(
                personId = "referee-2",
                firstName = secondRefereeName,
                lastName = "",
            ),
            rootCauseCounts = counts,
            comment = comment,
            history = history.map { it.toDto() },
        )
    }

    private fun CoachingHistoryEntry.toDto(): de.exhumedo.kmp.handball_support.client.CoachingHistoryEntryDto =
        de.exhumedo.kmp.handball_support.client.CoachingHistoryEntryDto(
            id = id,
            gameTimeMillis = gameTimeMillis,
            homeScore = homeScore,
            guestScore = guestScore,
            type = type.name,
            criterionId = criterionId,
            defectGroupId = defectGroupId,
            rootCauseId = rootCauseId,
            goalTeam = goalTeam?.name,
            selected = selected,
            team = attachment?.team?.name,
            teamLabel = attachment?.teamLabel,
            playerId = attachment?.playerId,
            playerLabel = attachment?.playerLabel,
            refereeName = attachment?.refereeName,
            note = note,
        )

    sealed interface SyncStatus {
        data object Idle : SyncStatus
        data object Syncing : SyncStatus
        data class Success(val message: String) : SyncStatus
        data class Offline(val message: String) : SyncStatus
        data class Error(val message: String) : SyncStatus
    }
}
