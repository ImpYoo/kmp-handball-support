package de.exhumedo.kmp.handball_support.domain

import de.exhumedo.kmp.handball_support.domain.model.HasVoteBy
import de.exhumedo.kmp.handball_support.domain.model.VoteByReferees
import de.exhumedo.kmp.handball_support.domain.usecase.EnrichMatchWithVoteStateUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoteStateUseCaseTest {

    private fun useCase(repo: InMemoryVoteRepository = InMemoryVoteRepository()) =
        EnrichMatchWithVoteStateUseCase(repo) to repo

    @Test
    fun freshMatchHasNoVotes() {
        val (useCase, _) = useCase()
        val result = useCase(matchWithDelegate())
        assertEquals(HasVoteBy(refereeTeam = false, delegate = false), result.hasVoteBy)
    }

    @Test
    fun matchWithRefereeTeamVoteReflectsRefereeTeamTrue() {
        val repo = InMemoryVoteRepository()
        val match = matchWithDelegate()
        val voteId = VoteByReferees.refereeVoteId(match.id, REF_A.id, REF_B.id)
        repo.save(
            VoteByReferees(
                id = voteId,
                matchId = match.id,
                phaseId = match.phaseId,
                phaseName = PHASE_NAME,
                timestamp = match.timestamp,
                homeTeam = HOME_TEAM,
                awayTeam = AWAY_TEAM,
                refereeA = REF_A,
                refereeB = REF_B,
                delegate = DELEGATE,
                timekeeper = TIMEKEEPER,
                scorekeeper = SCOREKEEPER,
                isVoteByDelegate = false,
                appearanceRating = 3,
                influenceRating = 3,
                teamworkRating = 3,
                comment = "",
            ),
        )
        val (useCase) = useCase(repo)
        val result = useCase(match)
        assertTrue(result.hasVoteBy.refereeTeam)
        assertFalse(result.hasVoteBy.delegate)
    }

    @Test
    fun matchWithDelegateVoteReflectsDelegateTrue() {
        val repo = InMemoryVoteRepository()
        val match = matchWithDelegate()
        val voteId = VoteByReferees.delegateVoteId(match.id, DELEGATE.id)
        repo.save(
            VoteByReferees(
                id = voteId,
                matchId = match.id,
                phaseId = match.phaseId,
                phaseName = PHASE_NAME,
                timestamp = match.timestamp,
                homeTeam = HOME_TEAM,
                awayTeam = AWAY_TEAM,
                refereeA = REF_A,
                refereeB = REF_B,
                delegate = DELEGATE,
                timekeeper = TIMEKEEPER,
                scorekeeper = SCOREKEEPER,
                isVoteByDelegate = true,
                appearanceRating = 2,
                influenceRating = 4,
                teamworkRating = 3,
                comment = "",
            ),
        )
        val (useCase) = useCase(repo)
        val result = useCase(match)
        assertFalse(result.hasVoteBy.refereeTeam)
        assertTrue(result.hasVoteBy.delegate)
    }

    @Test
    fun matchWithBothVotesReflectsBothTrue() {
        val repo = InMemoryVoteRepository()
        val match = matchWithDelegate()

        repo.save(
            VoteByReferees(
                id = VoteByReferees.refereeVoteId(match.id, REF_A.id, REF_B.id),
                matchId = match.id,
                phaseId = match.phaseId,
                phaseName = PHASE_NAME,
                timestamp = match.timestamp,
                homeTeam = HOME_TEAM,
                awayTeam = AWAY_TEAM,
                refereeA = REF_A,
                refereeB = REF_B,
                delegate = DELEGATE,
                timekeeper = TIMEKEEPER,
                scorekeeper = SCOREKEEPER,
                isVoteByDelegate = false,
                appearanceRating = 3,
                influenceRating = 3,
                teamworkRating = 3,
                comment = "",
            ),
        )
        repo.save(
            VoteByReferees(
                id = VoteByReferees.delegateVoteId(match.id, DELEGATE.id),
                matchId = match.id,
                phaseId = match.phaseId,
                phaseName = PHASE_NAME,
                timestamp = match.timestamp,
                homeTeam = HOME_TEAM,
                awayTeam = AWAY_TEAM,
                refereeA = REF_A,
                refereeB = REF_B,
                delegate = DELEGATE,
                timekeeper = TIMEKEEPER,
                scorekeeper = SCOREKEEPER,
                isVoteByDelegate = true,
                appearanceRating = 5,
                influenceRating = 5,
                teamworkRating = 5,
                comment = "",
            ),
        )

        val (useCase) = useCase(repo)
        val result = useCase(match)
        assertTrue(result.hasVoteBy.refereeTeam)
        assertTrue(result.hasVoteBy.delegate)
    }

    @Test
    fun matchWithoutDelegateAssignedAlwaysHasDelegateFalse() {
        val (useCase, _) = useCase()
        val result = useCase(matchWithoutDelegate())
        assertFalse(result.hasVoteBy.delegate)
    }
}
