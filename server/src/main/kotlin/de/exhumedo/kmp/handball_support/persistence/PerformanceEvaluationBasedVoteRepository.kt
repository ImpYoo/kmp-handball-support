package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.domain.model.PhaseName
import de.exhumedo.kmp.handball_support.domain.model.VoteByReferees
import de.exhumedo.kmp.handball_support.domain.model.Team as DomainTeam
import de.exhumedo.kmp.handball_support.domain.model.Person as DomainPerson
import de.exhumedo.kmp.handball_support.domain.rating.model.Evaluator
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluatorType
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import de.exhumedo.kmp.handball_support.domain.repository.VoteByRefereesRepository
import kotlinx.coroutines.runBlocking

/**
 * Adapts PerformanceEvaluationRepository to implement VoteByRefereesRepository.
 * Used exclusively by EnrichMatchWithVoteStateUseCase to check if a vote exists.
 *
 * ID format:
 *  - Referee team: "{matchId}-{refereeAId}-{refereeBId}" (3 segments)
 *  - Delegate:     "{matchId}-{delegateId}"              (2 segments)
 *
 * PerformanceEvaluation.game.gameId must equal matching Match.id.toString().
 */
class PerformanceEvaluationBasedVoteRepository(
    private val evaluationRepository: PerformanceEvaluationRepository,
) : VoteByRefereesRepository {

    override fun findById(id: String): VoteByReferees? = runBlocking {
        val parts = id.split("-")
        val matchId = parts.getOrNull(0) ?: return@runBlocking null
        val evaluatorType = if (parts.size >= 3) EvaluatorType.REFEREE_TEAM else EvaluatorType.DELEGATE
        val found = evaluationRepository.findByGameId(matchId).any { it.evaluator.type == evaluatorType }
        if (found) stubVote(id) else null
    }

    override fun findAll(): List<VoteByReferees> = runBlocking {
        evaluationRepository.findAll().map { eval ->
            val matchId = eval.game.gameId
            val voteId = when (eval.evaluator.type) {
                EvaluatorType.REFEREE_TEAM -> {
                    val pair = eval.evaluator as? Evaluator.RefereeTeam
                    val r1 = pair?.refereePair?.firstReferee?.person?.id.orEmpty()
                    val r2 = pair?.refereePair?.secondReferee?.person?.id.orEmpty()
                    "$matchId-$r1-$r2"
                }
                EvaluatorType.DELEGATE -> {
                    val d = eval.evaluator as? Evaluator.Delegate
                    val dId = d?.assignment?.person?.id.orEmpty()
                    "$matchId-$dId"
                }
            }
            stubVote(voteId)
        }
    }

    override fun save(vote: VoteByReferees): Unit =
        throw UnsupportedOperationException("Use PerformanceEvaluationApplicationService to save votes.")

    override fun deleteById(id: String): Boolean =
        throw UnsupportedOperationException("Deleting votes is not supported.")

    private fun stubVote(id: String) = VoteByReferees(
        id = id, matchId = 0, phaseId = 0,
        phaseName = PhaseName("", ""), timestamp = 0L,
        homeTeam = DomainTeam(0, ""), awayTeam = DomainTeam(0, ""),
        refereeA = DomainPerson(0, ""), refereeB = DomainPerson(0, ""),
        delegate = null, timekeeper = DomainPerson(0, ""), scorekeeper = DomainPerson(0, ""),
        isVoteByDelegate = false, appearanceRating = 0, influenceRating = 0, teamworkRating = 0,
    )
}
