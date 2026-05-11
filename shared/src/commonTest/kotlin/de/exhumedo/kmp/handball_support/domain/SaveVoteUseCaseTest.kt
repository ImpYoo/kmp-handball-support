package de.exhumedo.kmp.handball_support.domain

import de.exhumedo.kmp.handball_support.domain.model.CommentTooLongError
import de.exhumedo.kmp.handball_support.domain.model.DomainResult
import de.exhumedo.kmp.handball_support.domain.model.ForbiddenVoterError
import de.exhumedo.kmp.handball_support.domain.model.InvalidRatingError
import de.exhumedo.kmp.handball_support.domain.model.MissingDelegateError
import de.exhumedo.kmp.handball_support.domain.model.VoteAlreadyExistsError
import de.exhumedo.kmp.handball_support.domain.model.VoteByReferees
import de.exhumedo.kmp.handball_support.domain.usecase.RefereeVoteInput
import de.exhumedo.kmp.handball_support.domain.usecase.SaveVoteByRefereesUseCase
import de.exhumedo.kmp.handball_support.domain.usecase.VoteActor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SaveVoteUseCaseTest {

    private fun useCase(repo: InMemoryVoteRepository = InMemoryVoteRepository()) =
        SaveVoteByRefereesUseCase(repo) to repo

    // --- happy paths ---

    @Test
    fun validRefereeTeamVotePersistsAndReturnsSuccess() {
        val (useCase, repo) = useCase()
        val match = matchWithDelegate()
        val result = useCase(validRefereeTeamInput(match))
        assertIs<DomainResult.Success<VoteByReferees>>(result)
        assertFalse(result.value.isVoteByDelegate)
        assertEquals(VoteByReferees.refereeVoteId(match.id, REF_A.id, REF_B.id), result.value.id)
        assertEquals(1, repo.findAll().size)
    }

    @Test
    fun validDelegateVotePersistsAndReturnsSuccess() {
        val (useCase, repo) = useCase()
        val match = matchWithDelegate()
        val result = useCase(validDelegateInput(match))
        assertIs<DomainResult.Success<VoteByReferees>>(result)
        assertTrue(result.value.isVoteByDelegate)
        assertEquals(VoteByReferees.delegateVoteId(match.id, DELEGATE.id), result.value.id)
        assertEquals(1, repo.findAll().size)
    }

    @Test
    fun refereeBCanAlsoSubmitRefereeTeamVote() {
        val (useCase, _) = useCase()
        val match = matchWithDelegate()
        val result = useCase(validRefereeTeamInput(match, voterId = REF_B.id))
        assertIs<DomainResult.Success<VoteByReferees>>(result)
        assertEquals(VoteByReferees.refereeVoteId(match.id, REF_A.id, REF_B.id), result.value.id)
    }

    // --- duplicates ---

    @Test
    fun refereeTeamDuplicateVoteIsRejected() {
        val repo = InMemoryVoteRepository()
        val (useCase) = useCase(repo)
        val match = matchWithDelegate()
        val input = validRefereeTeamInput(match)
        useCase(input)
        val duplicate = useCase(input)
        assertIs<DomainResult.Failure>(duplicate)
        assertEquals(VoteAlreadyExistsError, duplicate.error)
        assertEquals(1, repo.findAll().size)
    }

    @Test
    fun delegateDuplicateVoteIsRejected() {
        val repo = InMemoryVoteRepository()
        val (useCase) = useCase(repo)
        val match = matchWithDelegate()
        val input = validDelegateInput(match)
        useCase(input)
        val duplicate = useCase(input)
        assertIs<DomainResult.Failure>(duplicate)
        assertEquals(VoteAlreadyExistsError, duplicate.error)
        assertEquals(1, repo.findAll().size)
    }

    // --- rating invariants ---

    @Test
    fun ratingBelowMinimumIsRejected() {
        val (useCase, _) = useCase()
        val result = useCase(validRefereeTeamInput(matchWithDelegate()).copy(appearanceRating = 0))
        assertIs<DomainResult.Failure>(result)
        assertEquals(InvalidRatingError, result.error)
    }

    @Test
    fun ratingAboveMaximumIsRejected() {
        val (useCase, _) = useCase()
        val result = useCase(validRefereeTeamInput(matchWithDelegate()).copy(influenceRating = 6))
        assertIs<DomainResult.Failure>(result)
        assertEquals(InvalidRatingError, result.error)
    }

    @Test
    fun singleOutOfRangeRatingRejectsEntireInput() {
        val (useCase, repo) = useCase()
        val result = useCase(
            validRefereeTeamInput(matchWithDelegate()).copy(
                appearanceRating = 5,
                influenceRating = 5,
                teamworkRating = 6,
            ),
        )
        assertIs<DomainResult.Failure>(result)
        assertEquals(InvalidRatingError, result.error)
        assertTrue(repo.findAll().isEmpty())
    }

    // --- comment invariant ---

    @Test
    fun commentExceedingMaxLengthIsRejected() {
        val (useCase, _) = useCase()
        val result = useCase(validRefereeTeamInput(matchWithDelegate()).copy(comment = "x".repeat(2_001)))
        assertIs<DomainResult.Failure>(result)
        assertEquals(CommentTooLongError, result.error)
    }

    @Test
    fun commentAtExactMaxLengthIsAccepted() {
        val (useCase, _) = useCase()
        val result = useCase(validRefereeTeamInput(matchWithDelegate()).copy(comment = "x".repeat(2_000)))
        assertIs<DomainResult.Success<VoteByReferees>>(result)
    }

    // --- authorization invariants ---

    @Test
    fun unknownVoterAsRefereeTeamIsRejectedWithForbiddenVoterError() {
        val (useCase, _) = useCase()
        val result = useCase(validRefereeTeamInput(matchWithDelegate(), voterId = 99))
        assertIs<DomainResult.Failure>(result)
        assertEquals(ForbiddenVoterError, result.error)
    }

    @Test
    fun wrongVoterIdAsDelegateIsRejectedWithForbiddenVoterError() {
        val (useCase, _) = useCase()
        val result = useCase(validDelegateInput(matchWithDelegate(), voterId = 99))
        assertIs<DomainResult.Failure>(result)
        assertEquals(ForbiddenVoterError, result.error)
    }

    @Test
    fun delegateVoteWhenMatchHasNoDelegateIsRejectedWithMissingDelegateError() {
        val (useCase, _) = useCase()
        val input = RefereeVoteInput(
            match = matchWithoutDelegate(),
            phaseName = PHASE_NAME,
            actor = VoteActor.Delegate(voterId = 20),
            appearanceRating = 3,
            influenceRating = 3,
            teamworkRating = 3,
            comment = "",
        )
        val result = useCase(input)
        assertIs<DomainResult.Failure>(result)
        assertEquals(MissingDelegateError, result.error)
    }
}
