package de.exhumedo.kmp.handball_support.application.coaching

import de.exhumedo.kmp.handball_support.persistence.coaching.CoachingEvaluationFilter
import de.exhumedo.kmp.handball_support.persistence.coaching.CoachingEvaluationRepository
import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingEvaluation
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingReport
import de.exhumedo.kmp.handball_support.referee_coaching.domain.scoring.CriterionScoringService
import de.exhumedo.kmp.handball_support.referee_coaching.domain.service.RefereeCoachingReportBuilder
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Orchestrates referee-coaching evaluations and report generation.
 */
class CoachingApplicationService(
    private val repository: CoachingEvaluationRepository,
    private val clock: Clock = Clock.System,
    private val idGenerator: CoachingEvaluationIdGenerator = UuidCoachingEvaluationIdGenerator(),
) {

    @OptIn(ExperimentalTime::class)
    fun create(command: CreateCoachingEvaluationCommand): RefereeCoachingEvaluation {
        val now = clock.now().toString()
        val criteria = applyCounts(DefaultCriterionCatalog().loadCriteria(), command.rootCauseCounts)
        val evaluation = RefereeCoachingEvaluation(
            id = idGenerator.nextId(),
            game = command.game,
            evaluatorUsername = command.evaluatorUsername,
            firstReferee = command.firstReferee,
            secondReferee = command.secondReferee,
            criteria = criteria,
            comment = command.comment,
            createdAt = now,
            updatedAt = now,
            history = command.history,
        )
        return repository.save(evaluation)
    }

    fun update(
        id: String,
        command: CreateCoachingEvaluationCommand,
    ): RefereeCoachingEvaluation? {
        val existing = repository.findById(id) ?: return null
        val criteria = applyCounts(DefaultCriterionCatalog().loadCriteria(), command.rootCauseCounts)
        val updated = existing.copy(
            game = command.game,
            evaluatorUsername = command.evaluatorUsername,
            firstReferee = command.firstReferee,
            secondReferee = command.secondReferee,
            criteria = criteria,
            comment = command.comment,
            updatedAt = kotlin.time.Clock.System.now().toString(),
            history = command.history,
        )
        return repository.save(updated)
    }

    fun findById(id: String): RefereeCoachingEvaluation? = repository.findById(id)

    fun findAll(filter: CoachingEvaluationFilter): List<RefereeCoachingEvaluation> = repository.findAll(filter)

    fun delete(id: String): Boolean = repository.deleteById(id)

    fun buildReport(id: String): RefereeCoachingReport? {
        val evaluation = repository.findById(id) ?: return null
        return RefereeCoachingReportBuilder().build(evaluation)
    }

    private fun applyCounts(
        criteria: List<de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion>,
        counts: Map<String, Map<String, Map<String, Int>>>,
    ): List<de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion> {
        val scoring = CriterionScoringService()
        return criteria.map { criterion ->
            val criterionCounts = counts[criterion.id] ?: return@map criterion
            criterion.defectGroups.fold(criterion) { groupAcc, group ->
                val groupCounts = criterionCounts[group.id] ?: return@fold groupAcc
                groupCounts.entries.fold(groupAcc) { rootAcc, (rootCauseId, count) ->
                    if (count == 0) return@fold rootAcc
                    if (count > 0) {
                        (1..count).fold(rootAcc) { acc, _ ->
                            scoring.incrementRootCause(acc, group.id, rootCauseId)
                        }
                    } else {
                        (1..-count).fold(rootAcc) { acc, _ ->
                            scoring.decrementRootCause(acc, group.id, rootCauseId)
                        }
                    }
                }
            }
        }
    }
}

fun interface CoachingEvaluationIdGenerator {
    fun nextId(): String
}

class UuidCoachingEvaluationIdGenerator : CoachingEvaluationIdGenerator {
    override fun nextId(): String = java.util.UUID.randomUUID().toString()
}
