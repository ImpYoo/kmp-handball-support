package de.exhumedo.kmp.handball_support.vote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.exhumedo.kmp.handball_support.client.MatchResponseDto
import de.exhumedo.kmp.handball_support.client.VoteApiClient
import de.exhumedo.kmp.handball_support.config.AppConfig

private const val DEFAULT_SCORE = "3"

/**
 * Holds the vote-form state: evaluator type, scores, comment, and submission.
 */
class VoteFormPresenter(
    private val api: VoteApiClient = VoteApiClient(),
) {
    var evaluatorType by mutableStateOf(VoteEvaluatorType.REFEREE_TEAM)
    var appearance by mutableStateOf(DEFAULT_SCORE)
    var influence by mutableStateOf(DEFAULT_SCORE)
    var teamwork by mutableStateOf(DEFAULT_SCORE)
    var comment by mutableStateOf("")
    var hasExistingVote by mutableStateOf(false)

    fun updateEvaluatorType(type: VoteEvaluatorType, match: MatchResponseDto?) {
        if (type == evaluatorType && match != null) return
        evaluatorType = type
        if (match != null) {
            hasExistingVote = when (type) {
                VoteEvaluatorType.REFEREE_TEAM -> match.hasVoteBy.refereeTeam
                VoteEvaluatorType.DELEGATE -> match.hasVoteBy.delegate
            }
        }
        resetScoreInputs()
        comment = ""
    }

    fun chooseMatch(match: MatchResponseDto) {
        evaluatorType = VoteEvaluatorType.REFEREE_TEAM
        comment = ""
        resetScoreInputs()
        hasExistingVote = match.hasVoteBy.refereeTeam
    }

    fun resetScoreInputs() {
        appearance = DEFAULT_SCORE
        influence = DEFAULT_SCORE
        teamwork = DEFAULT_SCORE
    }

    suspend fun submitVote(
        match: MatchResponseDto,
        token: String?,
        onBusyChange: (Boolean, String) -> Unit,
        onSuccess: (id: String, score: Int) -> Unit,
        onError: (Throwable, String) -> Unit,
    ) {
        val scores = parseScores()
        if (scores == null) {
            onBusyChange(false, "Scores must be between 1 and 5")
            return
        }

        onBusyChange(true, "Submitting vote...")
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
            onBusyChange(false, "Vote saved: id=${response.id}, score=${response.weightedTotalScore}")
            onSuccess(response.id, response.weightedTotalScore)
        } catch (e: Throwable) {
            onError(e, "Failed to submit vote")
        }
    }

    private fun parseScores(): ScoreInputs? {
        val a = appearance.toIntOrNull()
        val i = influence.toIntOrNull()
        val t = teamwork.toIntOrNull()
        if (a == null || i == null || t == null) return null
        if (a !in 1..5 || i !in 1..5 || t !in 1..5) return null
        return ScoreInputs(a, i, t)
    }

    private data class ScoreInputs(
        val appearance: Int,
        val influence: Int,
        val teamwork: Int,
    )
}