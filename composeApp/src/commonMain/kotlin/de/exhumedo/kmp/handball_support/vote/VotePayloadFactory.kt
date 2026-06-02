package de.exhumedo.kmp.handball_support.vote

import de.exhumedo.kmp.handball_support.client.CreatePerformanceEvaluationRequestDto
import de.exhumedo.kmp.handball_support.client.EvaluatorDto
import de.exhumedo.kmp.handball_support.client.GameDto
import de.exhumedo.kmp.handball_support.client.MatchPersonDto
import de.exhumedo.kmp.handball_support.client.MatchResponseDto
import de.exhumedo.kmp.handball_support.client.PersonDto
import de.exhumedo.kmp.handball_support.client.RefereePairDto
import de.exhumedo.kmp.handball_support.client.RoleAssignmentDto
import de.exhumedo.kmp.handball_support.client.ScoreRequestDto
import de.exhumedo.kmp.handball_support.client.TableOfficialTeamDto
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object VotePayloadFactory {
    fun build(
        match: MatchResponseDto,
        evaluatorType: VoteEvaluatorType,
        appearance: Int,
        influence: Int,
        teamwork: Int,
        comment: String,
    ): CreatePerformanceEvaluationRequestDto {
        val gameDate = Instant.fromEpochSeconds(match.timestamp)
            .toLocalDateTime(TimeZone.UTC)
            .date
            .toString()

        val refereeA = requireNotNull(match.refereeA) { "Match is missing the first referee." }
            .toRoleAssignment("FIRST_REFEREE")
        val refereeB = requireNotNull(match.refereeB) { "Match is missing the second referee." }
            .toRoleAssignment("SECOND_REFEREE")
        val timekeeper = requireNotNull(match.timekeeper) { "Match is missing the timekeeper." }
            .toRoleAssignment("TIME_KEEPER")
        val scorekeeper = requireNotNull(match.scorekeeper) { "Match is missing the scorekeeper." }
            .toRoleAssignment("SCORE_KEEPER")
        val delegate = match.delegate?.toRoleAssignment("DELEGATE")

        val evaluator = when (evaluatorType) {
            VoteEvaluatorType.REFEREE_TEAM -> EvaluatorDto(
                type = "REFEREE_TEAM",
                refereePair = RefereePairDto(
                    firstReferee = refereeA,
                    secondReferee = refereeB,
                ),
            )

            VoteEvaluatorType.DELEGATE -> {
                requireNotNull(delegate) { "Delegate vote requires a delegate in match data." }
                EvaluatorDto(type = "DELEGATE", delegate = delegate)
            }
        }

        return CreatePerformanceEvaluationRequestDto(
            game = GameDto(
                gameId = "${match.phaseId}-${match.id}",
                date = gameDate,
                homeTeam = match.homeTeam.name,
                awayTeam = match.awayTeam.name,
                venue = "Unknown",
            ),
            evaluator = evaluator,
            tableOfficialTeam = TableOfficialTeamDto(
                timeKeeper = timekeeper,
                scoreKeeper = scorekeeper,
                delegate = delegate,
            ),
            score = ScoreRequestDto(
                appearance = appearance,
                influence = influence,
                teamwork = teamwork,
            ),
            comment = comment,
        )
    }

    private fun MatchPersonDto.toRoleAssignment(role: String): RoleAssignmentDto {
        val (firstName, lastName) = splitName(name)
        return RoleAssignmentDto(
            person = PersonDto(
                id = id.toString(),
                firstName = firstName,
                lastName = lastName,
            ),
            role = role,
        )
    }

    internal fun splitName(fullName: String): Pair<String, String> {
        val normalized = fullName.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) return "Unknown" to "Official"
        val parts = normalized.split(" ", limit = 2)
        return if (parts.size == 1) {
            parts[0] to "Official"
        } else {
            parts[0] to parts[1]
        }
    }
}

enum class VoteEvaluatorType {
    REFEREE_TEAM,
    DELEGATE,
}

