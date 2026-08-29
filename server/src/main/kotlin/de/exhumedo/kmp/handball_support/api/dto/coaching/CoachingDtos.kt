package de.exhumedo.kmp.handball_support.api.dto.coaching

import de.exhumedo.kmp.handball_support.application.coaching.CreateCoachingEvaluationCommand
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingGame
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingPerson
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CriterionCategory
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingEvaluation
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingReport
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

@Serializable
data class CoachingCatalogResponseDto(
    val criteria: List<CatalogCriterionDto>,
)

@Serializable
data class CatalogCriterionDto(
    val id: String,
    val name: String,
    val category: String,
    val maxScore: Int,
    val defectGroups: List<CatalogDefectGroupDto>,
)

@Serializable
data class CatalogDefectGroupDto(
    val id: String,
    val name: String,
    val rootCauses: List<CatalogRootCauseDto>,
)

@Serializable
data class CatalogRootCauseDto(
    val id: String,
    val name: String,
)

fun CreateCoachingEvaluationRequestDto.toCommand(): CreateCoachingEvaluationCommand = CreateCoachingEvaluationCommand(
    game = CoachingGame(game.gameId, game.matchDate, game.homeTeam, game.awayTeam),
    evaluatorUsername = evaluatorUsername.trim(),
    firstReferee = CoachingPerson(firstReferee.personId, firstReferee.firstName, firstReferee.lastName),
    secondReferee = CoachingPerson(secondReferee.personId, secondReferee.firstName, secondReferee.lastName),
    rootCauseCounts = rootCauseCounts,
    comment = comment,
)

fun RefereeCoachingEvaluation.toResponseDto(): CoachingEvaluationResponseDto = CoachingEvaluationResponseDto(
    id = id,
    game = CoachingGameDto(game.gameId, game.matchDate, game.homeTeam, game.awayTeam),
    evaluatorUsername = evaluatorUsername,
    firstReferee = CoachingPersonDto(firstReferee.personId, firstReferee.firstName, firstReferee.lastName),
    secondReferee = CoachingPersonDto(secondReferee.personId, secondReferee.firstName, secondReferee.lastName),
    criteria = criteria.map { criterion ->
        CriterionScoreResponseDto(
            criterionId = criterion.id,
            criterionName = criterion.name,
            category = criterion.category.displayName,
            score = criterion.score,
            maxScore = 6,
            deductionPoints = 6 - criterion.score,
        )
    },
    totalScore = totalScore,
    maxTotalScore = maxTotalScore,
    percentage = percentage,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun RefereeCoachingReport.toResponseDto(): CoachingReportResponseDto = CoachingReportResponseDto(
    evaluationId = evaluationId,
    game = CoachingGameDto(game.gameId, game.matchDate, game.homeTeam, game.awayTeam),
    firstReferee = CoachingPersonDto(firstReferee.personId, firstReferee.firstName, firstReferee.lastName),
    secondReferee = CoachingPersonDto(secondReferee.personId, secondReferee.firstName, secondReferee.lastName),
    evaluatorUsername = evaluatorUsername,
    comment = comment,
    rows = rows.map { row ->
        CriterionReportRowDto(
            criterionId = row.criterionId,
            criterionName = row.criterionName,
            category = row.category.displayName,
            score = row.score,
            maxScore = row.maxScore,
            deductionPoints = row.deductionPoints,
            defectGroups = row.defectGroups.map { group ->
                DefectGroupReportRowDto(
                    groupId = group.groupId,
                    groupName = group.groupName,
                    selectedRootCauses = group.selectedRootCauses.map { rootCause ->
                        RootCauseReportRowDto(
                            rootCauseId = rootCause.rootCauseId,
                            rootCauseName = rootCause.rootCauseName,
                            count = rootCause.count,
                        )
                    },
                )
            },
        )
    },
    totalScore = totalScore,
    maxTotalScore = maxTotalScore,
    percentage = percentage,
)

fun List<de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion>.toCatalogDto(): CoachingCatalogResponseDto =
    CoachingCatalogResponseDto(
        criteria = map { criterion ->
            CatalogCriterionDto(
                id = criterion.id,
                name = criterion.name,
                category = criterion.category.displayName,
                maxScore = 6,
                defectGroups = criterion.defectGroups.map { group ->
                    CatalogDefectGroupDto(
                        id = group.id,
                        name = group.name,
                        rootCauses = group.rootCauses.map { rootCause ->
                            CatalogRootCauseDto(rootCause.id, rootCause.name)
                        },
                    )
                },
            )
        },
    )
