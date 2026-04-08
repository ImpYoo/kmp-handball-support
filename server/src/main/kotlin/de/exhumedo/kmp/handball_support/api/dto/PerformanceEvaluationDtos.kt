package de.exhumedo.kmp.handball_support.api.dto

import de.exhumedo.kmp.handball_support.application.CreatePerformanceEvaluationCommand
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Evaluator
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluatorType
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
 * Request payload for creating one performance evaluation.
 */
@Serializable
data class CreatePerformanceEvaluationRequestDto(
    val game: GameDto,
    val evaluator: EvaluatorDto,
    val tableOfficialTeam: TableOfficialTeamDto,
    val score: EvaluationScoreDto,
    val comment: String? = null,
)

/**
 * Response payload for one stored performance evaluation.
 */
@Serializable
data class PerformanceEvaluationResponseDto(
    val id: String,
    val game: GameDto,
    val evaluator: EvaluatorDto,
    val tableOfficialTeam: TableOfficialTeamDto,
    val score: EvaluationScoreDto,
    val weightedTotalScore: Int,
    val comment: String? = null,
    val createdAt: String,
)

/**
 * Transport representation of one game reference.
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
 * Transport representation of one person.
 */
@Serializable
data class PersonDto(
    val id: String,
    val firstName: String,
    val lastName: String,
)

/**
 * Transport enum for official roles.
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
 * Transport enum for evaluator kinds.
 */
@Serializable
enum class EvaluatorTypeDto {
    @SerialName("REFEREE_TEAM")
    REFEREE_TEAM,

    @SerialName("DELEGATE")
    DELEGATE,
}

/**
 * Transport representation of one contextual role assignment.
 */
@Serializable
data class RoleAssignmentDto(
    val person: PersonDto,
    val role: OfficialRoleDto,
)

/**
 * Transport representation of a referee pair.
 */
@Serializable
data class RefereePairDto(
    val firstReferee: RoleAssignmentDto,
    val secondReferee: RoleAssignmentDto,
)

/**
 * Transport representation of the evaluated table official team.
 */
@Serializable
data class TableOfficialTeamDto(
    val timeKeeper: RoleAssignmentDto,
    val scoreKeeper: RoleAssignmentDto,
    val delegate: RoleAssignmentDto? = null,
)

/**
 * Transport representation of the party submitting the evaluation.
 */
@Serializable
data class EvaluatorDto(
    val type: EvaluatorTypeDto,
    val refereePair: RefereePairDto? = null,
    val delegate: RoleAssignmentDto? = null,
)

/**
 * Transport representation of the evaluation score criteria.
 */
@Serializable
data class EvaluationScoreDto(
    val appearance: Int,
    val influence: Int,
    val teamwork: Int,
)

/**
 * Maps a validated request DTO into an application command.
 */
fun CreatePerformanceEvaluationRequestDto.toCreateCommand(): CreatePerformanceEvaluationCommand {
    require(game.gameId.isNotBlank()) { "game.gameId must not be blank" }
    require(game.date.isNotBlank()) { "game.date must not be blank" }
    require(game.homeTeam.isNotBlank()) { "game.homeTeam must not be blank" }
    require(game.awayTeam.isNotBlank()) { "game.awayTeam must not be blank" }
    require(game.venue.isNotBlank()) { "game.venue must not be blank" }

    fun requirePerson(person: PersonDto, context: String) {
        require(person.id.isNotBlank()) { "$context.id must not be blank" }
        require(person.firstName.isNotBlank()) { "$context.firstName must not be blank" }
        require(person.lastName.isNotBlank()) { "$context.lastName must not be blank" }
    }

    when (evaluator.type) {
        EvaluatorTypeDto.REFEREE_TEAM -> {
            val refereePair = requireNotNull(evaluator.refereePair) {
                "evaluator.refereePair is required for evaluator.type=REFEREE_TEAM"
            }
            require(evaluator.delegate == null) {
                "evaluator.delegate must be null for evaluator.type=REFEREE_TEAM"
            }
            requirePerson(refereePair.firstReferee.person, "evaluator.refereePair.firstReferee.person")
            requirePerson(refereePair.secondReferee.person, "evaluator.refereePair.secondReferee.person")
        }

        EvaluatorTypeDto.DELEGATE -> {
            val delegate = requireNotNull(evaluator.delegate) {
                "evaluator.delegate is required for evaluator.type=DELEGATE"
            }
            require(evaluator.refereePair == null) {
                "evaluator.refereePair must be null for evaluator.type=DELEGATE"
            }
            requirePerson(delegate.person, "evaluator.delegate.person")
        }
    }

    requirePerson(tableOfficialTeam.timeKeeper.person, "tableOfficialTeam.timeKeeper.person")
    requirePerson(tableOfficialTeam.scoreKeeper.person, "tableOfficialTeam.scoreKeeper.person")
    tableOfficialTeam.delegate?.let { requirePerson(it.person, "tableOfficialTeam.delegate.person") }

    require(score.appearance in Score.MIN_VALUE..Score.MAX_VALUE) {
        "score.appearance must be between ${Score.MIN_VALUE} and ${Score.MAX_VALUE}"
    }
    require(score.influence in Score.MIN_VALUE..Score.MAX_VALUE) {
        "score.influence must be between ${Score.MIN_VALUE} and ${Score.MAX_VALUE}"
    }
    require(score.teamwork in Score.MIN_VALUE..Score.MAX_VALUE) {
        "score.teamwork must be between ${Score.MIN_VALUE} and ${Score.MAX_VALUE}"
    }

    val normalizedComment = comment?.takeIf { it.isNotBlank() }?.also {
        require(it.length <= COMMENT_MAX_LENGTH) {
            "comment must not exceed $COMMENT_MAX_LENGTH characters"
        }
    }

    return CreatePerformanceEvaluationCommand(
        game = game.toDomain(),
        evaluator = evaluator.toDomain(),
        tableOfficialTeam = tableOfficialTeam.toDomain(),
        score = score.toDomain(),
        comment = normalizedComment,
    )
}

private const val COMMENT_MAX_LENGTH = 2000

/**
 * Maps a domain aggregate into the public response DTO.
 */
fun PerformanceEvaluation.toResponseDto(): PerformanceEvaluationResponseDto {
    return PerformanceEvaluationResponseDto(
        id = id,
        game = game.toDto(),
        evaluator = evaluator.toDto(),
        tableOfficialTeam = tableOfficialTeam.toDto(),
        score = score.toDto(),
        weightedTotalScore = score.toScore(),
        comment = comment,
        createdAt = createdAt,
    )
}

/**
 * Maps a game DTO to the domain model.
 */
private fun GameDto.toDomain(): Game = Game(
    gameId = gameId,
    date = date,
    homeTeam = homeTeam,
    awayTeam = awayTeam,
    venue = venue,
)

/**
 * Maps a domain game to the transport DTO.
 */
private fun Game.toDto(): GameDto = GameDto(
    gameId = gameId,
    date = date,
    homeTeam = homeTeam,
    awayTeam = awayTeam,
    venue = venue,
)

/**
 * Maps a person DTO to the domain model.
 */
private fun PersonDto.toDomain(): Person = Person(
    id = id,
    firstName = firstName,
    lastName = lastName,
)

/**
 * Maps a domain person to the transport DTO.
 */
private fun Person.toDto(): PersonDto = PersonDto(
    id = id,
    firstName = firstName,
    lastName = lastName,
)

/**
 * Maps a role assignment DTO to the domain model.
 */
private fun RoleAssignmentDto.toDomain(): RoleAssignment = RoleAssignment(
    person = person.toDomain(),
    role = role.toDomain(),
)

/**
 * Maps a domain role assignment to the transport DTO.
 */
private fun RoleAssignment.toDto(): RoleAssignmentDto = RoleAssignmentDto(
    person = person.toDto(),
    role = role.toDto(),
)

/**
 * Maps a referee pair DTO to the domain model.
 */
private fun RefereePairDto.toDomain(): RefereePair = RefereePair(
    firstReferee = firstReferee.toDomain(),
    secondReferee = secondReferee.toDomain(),
)

/**
 * Maps a domain referee pair to the transport DTO.
 */
private fun RefereePair.toDto(): RefereePairDto = RefereePairDto(
    firstReferee = firstReferee.toDto(),
    secondReferee = secondReferee.toDto(),
)

/**
 * Maps a table official team DTO to the domain model.
 */
private fun TableOfficialTeamDto.toDomain(): TableOfficialTeam = TableOfficialTeam(
    timeKeeper = timeKeeper.toDomain(),
    scoreKeeper = scoreKeeper.toDomain(),
    delegate = delegate?.toDomain(),
)

/**
 * Maps a domain table official team to the transport DTO.
 */
private fun TableOfficialTeam.toDto(): TableOfficialTeamDto = TableOfficialTeamDto(
    timeKeeper = timeKeeper.toDto(),
    scoreKeeper = scoreKeeper.toDto(),
    delegate = delegate?.toDto(),
)

/**
 * Maps an evaluator DTO to the domain model.
 */
private fun EvaluatorDto.toDomain(): Evaluator = when (type) {
    EvaluatorTypeDto.REFEREE_TEAM -> Evaluator.RefereeTeam(
        refereePair = requireNotNull(refereePair) { "evaluator.refereePair is required" }.toDomain(),
    )

    EvaluatorTypeDto.DELEGATE -> Evaluator.Delegate(
        assignment = requireNotNull(delegate) { "evaluator.delegate is required" }.toDomain(),
    )
}

/**
 * Maps a domain evaluator to the transport DTO.
 */
private fun Evaluator.toDto(): EvaluatorDto = when (this) {
    is Evaluator.RefereeTeam -> EvaluatorDto(
        type = EvaluatorTypeDto.REFEREE_TEAM,
        refereePair = refereePair.toDto(),
        delegate = null,
    )

    is Evaluator.Delegate -> EvaluatorDto(
        type = EvaluatorTypeDto.DELEGATE,
        refereePair = null,
        delegate = assignment.toDto(),
    )
}

/**
 * Maps a score DTO to the domain model.
 */
private fun EvaluationScoreDto.toDomain(): EvaluationScore = EvaluationScore(
    appearance = Score(appearance),
    influence = Score(influence),
    teamwork = Score(teamwork),
)

/**
 * Maps a domain score to the transport DTO.
 */
private fun EvaluationScore.toDto(): EvaluationScoreDto = EvaluationScoreDto(
    appearance = appearance.value,
    influence = influence.value,
    teamwork = teamwork.value,
)

/**
 * Maps a domain evaluator type to the transport enum.
 */
private fun EvaluatorType.toDto(): EvaluatorTypeDto = when (this) {
    EvaluatorType.REFEREE_TEAM -> EvaluatorTypeDto.REFEREE_TEAM
    EvaluatorType.DELEGATE -> EvaluatorTypeDto.DELEGATE
}

/**
 * Maps a transport official role to the domain role.
 */
private fun OfficialRoleDto.toDomain(): OfficialRole = when (this) {
    OfficialRoleDto.FIRST_REFEREE -> OfficialRole.FirstReferee
    OfficialRoleDto.SECOND_REFEREE -> OfficialRole.SecondReferee
    OfficialRoleDto.TIME_KEEPER -> OfficialRole.TimeKeeper
    OfficialRoleDto.SCORE_KEEPER -> OfficialRole.ScoreKeeper
    OfficialRoleDto.DELEGATE -> OfficialRole.Delegate
}

/**
 * Maps a domain official role to the transport enum.
 */
private fun OfficialRole.toDto(): OfficialRoleDto = when (this) {
    OfficialRole.FirstReferee -> OfficialRoleDto.FIRST_REFEREE
    OfficialRole.SecondReferee -> OfficialRoleDto.SECOND_REFEREE
    OfficialRole.TimeKeeper -> OfficialRoleDto.TIME_KEEPER
    OfficialRole.ScoreKeeper -> OfficialRoleDto.SCORE_KEEPER
    OfficialRole.Delegate -> OfficialRoleDto.DELEGATE
}
