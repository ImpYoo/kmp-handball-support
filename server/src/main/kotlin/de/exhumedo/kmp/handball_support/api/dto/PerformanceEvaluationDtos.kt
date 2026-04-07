package de.exhumedo.kmp.handball_support.api.dto

import de.exhumedo.kmp.handball_support.application.CreatePerformanceEvaluationCommand
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Game
import de.exhumedo.kmp.handball_support.domain.rating.model.OfficialRole
import de.exhumedo.kmp.handball_support.domain.rating.model.PerformanceEvaluation
import de.exhumedo.kmp.handball_support.domain.rating.model.Person
import de.exhumedo.kmp.handball_support.domain.rating.model.RefereePair
import de.exhumedo.kmp.handball_support.domain.rating.model.RoleAssignment
import de.exhumedo.kmp.handball_support.domain.rating.model.Score
import de.exhumedo.kmp.handball_support.domain.rating.model.TableOfficialTeam
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTO for creating a performance evaluation.
 *
 * @property game Referenced game data.
 * @property refereePair Referee pair acting as evaluator.
 * @property tableOfficialTeam Evaluated table official team.
 * @property score Raw criterion scores.
 * @property comment Optional comment.
 */
@Serializable
data class CreatePerformanceEvaluationRequestDto(
    val game: GameDto,
    val refereePair: RefereePairDto,
    val tableOfficialTeam: TableOfficialTeamDto,
    val score: EvaluationScoreDto,
    val comment: String? = null,
)

/**
 * Response DTO for a stored performance evaluation.
 *
 * @property id Evaluation identifier.
 * @property game Referenced game data.
 * @property refereePair Referee pair acting as evaluator.
 * @property tableOfficialTeam Evaluated table official team.
 * @property score Raw criterion scores.
 * @property weightedTotalScore Derived score using default weights.
 * @property comment Optional comment.
 * @property createdAt ISO 8601 creation timestamp.
 */
@Serializable
data class PerformanceEvaluationResponseDto(
    val id: String,
    val game: GameDto,
    val refereePair: RefereePairDto,
    val tableOfficialTeam: TableOfficialTeamDto,
    val score: EvaluationScoreDto,
    val weightedTotalScore: Int,
    val comment: String? = null,
    val createdAt: String,
)

/**
 * DTO for a referenced game.
 */
@Serializable
data class GameDto(
    val gameId: String,
    val date: String,
    val homeTeam: String,
    val awayTeam: String,
    val venue: String,
)

/**
 * DTO for a person.
 */
@Serializable
data class PersonDto(
    val id: String,
    val firstName: String,
    val lastName: String,
)

/**
 * DTO for an official role.
 */
@Serializable
enum class OfficialRoleDto {
    @SerialName("FIRST_REFEREE")
    FIRST_REFEREE,

    @SerialName("SECOND_REFEREE")
    SECOND_REFEREE,

    @SerialName("TIME_KEEPER")
    TIME_KEEPER,

    @SerialName("SCORE_KEEPER")
    SCORE_KEEPER,

    @SerialName("DELEGATE")
    DELEGATE,
}

/**
 * DTO binding a person to a role.
 */
@Serializable
data class RoleAssignmentDto(
    val person: PersonDto,
    val role: OfficialRoleDto,
)

/**
 * DTO for a referee pair.
 */
@Serializable
data class RefereePairDto(
    val firstReferee: RoleAssignmentDto,
    val secondReferee: RoleAssignmentDto,
)

/**
 * DTO for a table official team.
 */
@Serializable
data class TableOfficialTeamDto(
    val timeKeeper: RoleAssignmentDto,
    val scoreKeeper: RoleAssignmentDto,
    val delegate: RoleAssignmentDto? = null,
)

/**
 * DTO for raw evaluation criteria.
 */
@Serializable
data class EvaluationScoreDto(
    val appearance: Int,
    val influence: Int,
    val teamwork: Int,
)

/**
 * Maps the incoming request DTO into an application command.
 *
 * @return The mapped application command.
 */
fun CreatePerformanceEvaluationRequestDto.toCreateCommand(): CreatePerformanceEvaluationCommand {
    val normalizedComment = comment?.takeIf { it.isNotBlank() }

    return CreatePerformanceEvaluationCommand(
        game = game.toDomain(),
        refereePair = refereePair.toDomain(),
        tableOfficialTeam = tableOfficialTeam.toDomain(),
        score = score.toDomain(),
        comment = normalizedComment,
    )
}

/**
 * Maps the domain aggregate into the response DTO.
 *
 * @return The serialized response representation.
 */
fun PerformanceEvaluation.toResponseDto(): PerformanceEvaluationResponseDto {
    return PerformanceEvaluationResponseDto(
        id = id,
        game = game.toDto(),
        refereePair = refereePair.toDto(),
        tableOfficialTeam = tableOfficialTeam.toDto(),
        score = score.toDto(),
        weightedTotalScore = score.toScore(),
        comment = comment,
        createdAt = createdAt,
    )
}

private fun GameDto.toDomain(): Game = Game(
    gameId = gameId,
    date = date,
    homeTeam = homeTeam,
    awayTeam = awayTeam,
    venue = venue,
)

private fun Game.toDto(): GameDto = GameDto(
    gameId = gameId,
    date = date,
    homeTeam = homeTeam,
    awayTeam = awayTeam,
    venue = venue,
)

private fun PersonDto.toDomain(): Person = Person(
    id = id,
    firstName = firstName,
    lastName = lastName,
)

private fun Person.toDto(): PersonDto = PersonDto(
    id = id,
    firstName = firstName,
    lastName = lastName,
)

private fun RoleAssignmentDto.toDomain(): RoleAssignment = RoleAssignment(
    person = person.toDomain(),
    role = role.toDomain(),
)

private fun RoleAssignment.toDto(): RoleAssignmentDto = RoleAssignmentDto(
    person = person.toDto(),
    role = role.toDto(),
)

private fun RefereePairDto.toDomain(): RefereePair = RefereePair(
    firstReferee = firstReferee.toDomain(),
    secondReferee = secondReferee.toDomain(),
)

private fun RefereePair.toDto(): RefereePairDto = RefereePairDto(
    firstReferee = firstReferee.toDto(),
    secondReferee = secondReferee.toDto(),
)

private fun TableOfficialTeamDto.toDomain(): TableOfficialTeam = TableOfficialTeam(
    timeKeeper = timeKeeper.toDomain(),
    scoreKeeper = scoreKeeper.toDomain(),
    delegate = delegate?.toDomain(),
)

private fun TableOfficialTeam.toDto(): TableOfficialTeamDto = TableOfficialTeamDto(
    timeKeeper = timeKeeper.toDto(),
    scoreKeeper = scoreKeeper.toDto(),
    delegate = delegate?.toDto(),
)

private fun EvaluationScoreDto.toDomain(): EvaluationScore = EvaluationScore(
    appearance = Score(appearance),
    influence = Score(influence),
    teamwork = Score(teamwork),
)

private fun EvaluationScore.toDto(): EvaluationScoreDto = EvaluationScoreDto(
    appearance = appearance.value,
    influence = influence.value,
    teamwork = teamwork.value,
)

private fun OfficialRoleDto.toDomain(): OfficialRole = when (this) {
    OfficialRoleDto.FIRST_REFEREE -> OfficialRole.FirstReferee
    OfficialRoleDto.SECOND_REFEREE -> OfficialRole.SecondReferee
    OfficialRoleDto.TIME_KEEPER -> OfficialRole.TimeKeeper
    OfficialRoleDto.SCORE_KEEPER -> OfficialRole.ScoreKeeper
    OfficialRoleDto.DELEGATE -> OfficialRole.Delegate
}

private fun OfficialRole.toDto(): OfficialRoleDto = when (this) {
    OfficialRole.FirstReferee -> OfficialRoleDto.FIRST_REFEREE
    OfficialRole.SecondReferee -> OfficialRoleDto.SECOND_REFEREE
    OfficialRole.TimeKeeper -> OfficialRoleDto.TIME_KEEPER
    OfficialRole.ScoreKeeper -> OfficialRoleDto.SCORE_KEEPER
    OfficialRole.Delegate -> OfficialRoleDto.DELEGATE
}
