package de.exhumedo.kmp.handball_support.client

import kotlinx.serialization.Serializable

@Serializable
data class TokenRequestDto(
    val username: String,
    val password: String,
)

@Serializable
data class TokenResponseDto(
    val accessToken: String,
    val tokenType: String,
    val expiresAt: String,
    val role: String,
)

@Serializable
data class PhaseResponseDto(
    val phaseId: Int,
    val tournamentId: Int,
    val seasonId: Int,
    val name: String,
    val shortName: String,
    val matchDays: List<MatchDayResponseDto> = emptyList(),
)

@Serializable
data class MatchDayResponseDto(
    val id: Int,
    val matches: List<MatchResponseDto> = emptyList(),
)

@Serializable
data class MatchResponseDto(
    val id: Int,
    val phaseId: Int,
    val timestamp: Long,
    val homeTeam: TeamDto,
    val awayTeam: TeamDto,
    val refereeA: MatchPersonDto? = null,
    val refereeB: MatchPersonDto? = null,
    val timekeeper: MatchPersonDto? = null,
    val scorekeeper: MatchPersonDto? = null,
    val delegate: MatchPersonDto? = null,
    val result: String = "",
    val halftimeResult: String = "",
    val hasVoteBy: HasVoteByDto,
)

@Serializable
data class TeamDto(
    val id: Int,
    val name: String,
)

@Serializable
data class MatchPersonDto(
    val id: Int,
    val name: String,
)

@Serializable
data class HasVoteByDto(
    val refereeTeam: Boolean,
    val delegate: Boolean,
)

@Serializable
data class PersonDto(
    val id: String,
    val firstName: String,
    val lastName: String,
)

@Serializable
data class RoleAssignmentDto(
    val person: PersonDto,
    val role: String,
)

@Serializable
data class RefereePairDto(
    val firstReferee: RoleAssignmentDto,
    val secondReferee: RoleAssignmentDto,
)

@Serializable
data class TableOfficialTeamDto(
    val timeKeeper: RoleAssignmentDto,
    val scoreKeeper: RoleAssignmentDto,
    val delegate: RoleAssignmentDto? = null,
)

@Serializable
data class ScoreRequestDto(
    val appearance: Int,
    val influence: Int,
    val teamwork: Int,
)

@Serializable
data class EvaluatorDto(
    val type: String,
    val refereePair: RefereePairDto? = null,
    val delegate: RoleAssignmentDto? = null,
)

@Serializable
data class GameDto(
    val gameId: String,
    val date: String,
    val homeTeam: String,
    val awayTeam: String,
    val venue: String,
)

@Serializable
data class CreatePerformanceEvaluationRequestDto(
    val game: GameDto,
    val evaluator: EvaluatorDto,
    val tableOfficialTeam: TableOfficialTeamDto,
    val score: ScoreRequestDto,
    val comment: String = "",
)

@Serializable
data class PerformanceEvaluationResponseDto(
    val id: String,
    val gameId: String,
    val evaluatorType: String,
    val weightedTotalScore: Int,
    val createdAt: String,
)

