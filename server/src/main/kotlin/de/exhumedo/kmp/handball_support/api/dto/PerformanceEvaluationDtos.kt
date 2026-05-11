package de.exhumedo.kmp.handball_support.api.dto

import de.exhumedo.kmp.handball_support.application.CreatePerformanceEvaluationCommand
import de.exhumedo.kmp.handball_support.domain.rating.model.Evaluator
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Game
import de.exhumedo.kmp.handball_support.domain.rating.model.OfficialRole
import de.exhumedo.kmp.handball_support.domain.rating.model.PerformanceEvaluation
import de.exhumedo.kmp.handball_support.domain.rating.model.Person
import de.exhumedo.kmp.handball_support.domain.rating.model.RefereePair
import de.exhumedo.kmp.handball_support.domain.rating.model.RoleAssignment
import de.exhumedo.kmp.handball_support.domain.rating.model.Score
import de.exhumedo.kmp.handball_support.domain.rating.model.TableOfficialTeam
import kotlinx.serialization.Serializable

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
    val gameDate: String,
    val homeTeam: String,
    val awayTeam: String,
    val venue: String,
    val evaluatorType: String,
    val appearance: Int,
    val influence: Int,
    val teamwork: Int,
    val weightedTotalScore: Int,
    val comment: String,
    val createdAt: String,
)

fun CreatePerformanceEvaluationRequestDto.toCreateCommand(): CreatePerformanceEvaluationCommand {
    validateRequest(this)
    return CreatePerformanceEvaluationCommand(
        game = Game(game.gameId, game.date, game.homeTeam, game.awayTeam, game.venue),
        evaluator = when (evaluator.type) {
            "REFEREE_TEAM" -> {
                val pair = requireNotNull(evaluator.refereePair) { "refereePair is required for REFEREE_TEAM evaluator" }
                Evaluator.RefereeTeam(RefereePair(toDomain(pair.firstReferee), toDomain(pair.secondReferee)))
            }
            "DELEGATE" -> {
                val d = requireNotNull(evaluator.delegate) { "delegate is required for DELEGATE evaluator" }
                Evaluator.Delegate(toDomain(d))
            }
            else -> throw IllegalArgumentException("Unknown evaluator type: ${evaluator.type}")
        },
        tableOfficialTeam = TableOfficialTeam(
            timeKeeper = toDomain(tableOfficialTeam.timeKeeper),
            scoreKeeper = toDomain(tableOfficialTeam.scoreKeeper),
            delegate = tableOfficialTeam.delegate?.let { toDomain(it) },
        ),
        score = EvaluationScore(Score(score.appearance), Score(score.influence), Score(score.teamwork)),
        comment = comment,
    )
}

fun PerformanceEvaluation.toResponseDto(): PerformanceEvaluationResponseDto = PerformanceEvaluationResponseDto(
    id = id,
    gameId = game.gameId,
    gameDate = game.date,
    homeTeam = game.homeTeam,
    awayTeam = game.awayTeam,
    venue = game.venue,
    evaluatorType = evaluator.type.name,
    appearance = score.appearance.value,
    influence = score.influence.value,
    teamwork = score.teamwork.value,
    weightedTotalScore = score.toScore(),
    comment = comment,
    createdAt = createdAt,
)

private fun validateRequest(dto: CreatePerformanceEvaluationRequestDto) {
    require(dto.game.gameId.isNotBlank()) { "game.gameId must not be blank" }
}

private fun toDomain(dto: RoleAssignmentDto): RoleAssignment {
    val role: OfficialRole = when (dto.role) {
        "FIRST_REFEREE" -> OfficialRole.FirstReferee
        "SECOND_REFEREE" -> OfficialRole.SecondReferee
        "DELEGATE" -> OfficialRole.Delegate
        "TIME_KEEPER" -> OfficialRole.TimeKeeper
        "SCORE_KEEPER" -> OfficialRole.ScoreKeeper
        else -> throw IllegalArgumentException("Unknown role: ${dto.role}")
    }
    return RoleAssignment(Person(dto.person.id, dto.person.firstName, dto.person.lastName), role)
}
