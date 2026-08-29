package de.exhumedo.kmp.handball_support.client

import kotlinx.serialization.Serializable

@Serializable
data class CoachingPersonDto(
    val personId: String,
    val firstName: String,
    val lastName: String,
)

@Serializable
data class CoachingGameDto(
    val gameId: String,
    val matchDate: String,
    val homeTeam: String,
    val awayTeam: String,
)

@Serializable
data class CreateCoachingEvaluationRequestDto(
    val game: CoachingGameDto,
    val evaluatorUsername: String,
    val firstReferee: CoachingPersonDto,
    val secondReferee: CoachingPersonDto,
    val rootCauseCounts: Map<String, Map<String, Map<String, Int>>>,
    val comment: String = "",
    val history: List<CoachingHistoryEntryDto> = emptyList(),
)

@Serializable
data class CoachingEvaluationResponseDto(
    val id: String,
    val game: CoachingGameDto,
    val evaluatorUsername: String,
    val firstReferee: CoachingPersonDto,
    val secondReferee: CoachingPersonDto,
    val criteria: List<CriterionScoreResponseDto>,
    val totalScore: Int,
    val maxTotalScore: Int,
    val percentage: Int,
    val comment: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CriterionScoreResponseDto(
    val criterionId: String,
    val criterionName: String,
    val category: String,
    val score: Int,
    val maxScore: Int,
    val deductionPoints: Int,
)

@Serializable
data class CoachingReportResponseDto(
    val evaluationId: String,
    val game: CoachingGameDto,
    val firstReferee: CoachingPersonDto,
    val secondReferee: CoachingPersonDto,
    val evaluatorUsername: String,
    val comment: String,
    val rows: List<CriterionReportRowDto>,
    val totalScore: Int,
    val maxTotalScore: Int,
    val percentage: Int,
    val history: List<CoachingHistoryEntryDto> = emptyList(),
)

@Serializable
data class CoachingHistoryEntryDto(
    val id: String,
    val gameTimeMillis: Long,
    val homeScore: Int,
    val guestScore: Int,
    val type: String,
    val criterionId: String? = null,
    val defectGroupId: String? = null,
    val rootCauseId: String? = null,
    val goalTeam: String? = null,
    val selected: Boolean = true,
    val team: String? = null,
    val teamLabel: String? = null,
    val playerId: String? = null,
    val playerLabel: String? = null,
    val refereeName: String? = null,
    val note: String = "",
)

@Serializable
data class CriterionReportRowDto(
    val criterionId: String,
    val criterionName: String,
    val category: String,
    val score: Int,
    val maxScore: Int,
    val deductionPoints: Int,
    val defectGroups: List<DefectGroupReportRowDto>,
)

@Serializable
data class DefectGroupReportRowDto(
    val groupId: String,
    val groupName: String,
    val selectedRootCauses: List<RootCauseReportRowDto>,
)

@Serializable
data class RootCauseReportRowDto(
    val rootCauseId: String,
    val rootCauseName: String,
    val count: Int,
)
