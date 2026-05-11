package de.exhumedo.kmp.handball_support.application

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import de.exhumedo.kmp.handball_support.domain.rating.model.Evaluator
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Game
import de.exhumedo.kmp.handball_support.domain.rating.model.OfficialRole
import de.exhumedo.kmp.handball_support.domain.rating.model.PerformanceEvaluation
import de.exhumedo.kmp.handball_support.domain.rating.model.TableOfficialTeam
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import java.util.UUID
import kotlin.time.Clock

interface EvaluationIdGenerator {
    fun newId(): String
}

class UuidEvaluationIdGenerator : EvaluationIdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}

class PerformanceEvaluationApplicationService(
    private val repository: PerformanceEvaluationRepository,
    private val idGenerator: EvaluationIdGenerator,
    private val clock: Clock = Clock.System,
) {
    fun create(
        game: Game,
        evaluator: Evaluator,
        tableOfficialTeam: TableOfficialTeam,
        score: EvaluationScore,
        comment: String = "",
    ): PerformanceEvaluation {
        if (comment.length > PerformanceEvaluation.MAX_COMMENT_LENGTH) {
            throw DomainException.CommentTooLong(comment.length, PerformanceEvaluation.MAX_COMMENT_LENGTH)
        }

        val existingForGame = repository.findByGameId(game.gameId)
        val duplicate = existingForGame.any { it.evaluator.type == evaluator.type }
        if (duplicate) {
            throw DomainException.DuplicateGameEvaluation(game.gameId, evaluator.type.name)
        }

        when (evaluator) {
            is Evaluator.RefereeTeam -> {
                val r1 = evaluator.refereePair.firstReferee
                if (r1.role != OfficialRole.FirstReferee) {
                    throw DomainException.InvalidRoleAssignment("FirstReferee", r1.role::class.simpleName ?: r1.role.toString(), r1.person.id)
                }
                val r2 = evaluator.refereePair.secondReferee
                if (r2.role != OfficialRole.SecondReferee) {
                    throw DomainException.InvalidRoleAssignment("SecondReferee", r2.role::class.simpleName ?: r2.role.toString(), r2.person.id)
                }
            }
            is Evaluator.Delegate -> {
                val d = evaluator.assignment
                if (d.role != OfficialRole.Delegate) {
                    throw DomainException.InvalidRoleAssignment("Delegate", d.role::class.simpleName ?: d.role.toString(), d.person.id)
                }
            }
        }

        val evaluation = PerformanceEvaluation(
            id = idGenerator.newId(),
            game = game,
            evaluator = evaluator,
            tableOfficialTeam = tableOfficialTeam,
            score = score,
            comment = comment,
            createdAt = clock.now().toString(),
        )
        return repository.save(evaluation)
    }
}
