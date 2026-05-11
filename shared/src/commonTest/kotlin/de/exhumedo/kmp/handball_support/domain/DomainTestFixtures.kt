package de.exhumedo.kmp.handball_support.domain

import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.Person
import de.exhumedo.kmp.handball_support.domain.model.PhaseName
import de.exhumedo.kmp.handball_support.domain.model.Team
import de.exhumedo.kmp.handball_support.domain.model.VoteByReferees
import de.exhumedo.kmp.handball_support.domain.repository.VoteByRefereesRepository
import de.exhumedo.kmp.handball_support.domain.usecase.RefereeVoteInput
import de.exhumedo.kmp.handball_support.domain.usecase.VoteActor

// ---------------------------------------------------------------------------
// shared domain fixtures
// ---------------------------------------------------------------------------

internal val REF_A = Person(id = 10, name = "Referee A")
internal val REF_B = Person(id = 11, name = "Referee B")
internal val DELEGATE = Person(id = 20, name = "Delegate")
internal val TIMEKEEPER = Person(id = 30, name = "Timekeeper")
internal val SCOREKEEPER = Person(id = 31, name = "Scorekeeper")
internal val HOME_TEAM = Team(id = 1, name = "Home")
internal val AWAY_TEAM = Team(id = 2, name = "Away")
internal val PHASE_NAME = PhaseName(fullName = "Bundesliga 24/25", shortName = "BL")

internal fun matchWithDelegate(matchId: Int = 100): Match = Match(
    id = matchId,
    tournamentId = 1,
    seasonId = 1,
    phaseId = 1,
    timestamp = 1_700_000_000L,
    homeTeam = HOME_TEAM,
    awayTeam = AWAY_TEAM,
    refereeA = REF_A,
    refereeB = REF_B,
    timekeeper = TIMEKEEPER,
    scorekeeper = SCOREKEEPER,
    delegate = DELEGATE,
)

internal fun matchWithoutDelegate(matchId: Int = 200): Match = matchWithDelegate(matchId).copy(delegate = null)

internal fun validRefereeTeamInput(match: Match, voterId: Int = REF_A.id): RefereeVoteInput = RefereeVoteInput(
    match = match,
    phaseName = PHASE_NAME,
    actor = VoteActor.RefereeTeam(voterId),
    appearanceRating = 3,
    influenceRating = 4,
    teamworkRating = 5,
    comment = "Good performance",
)

internal fun validDelegateInput(match: Match, voterId: Int = DELEGATE.id): RefereeVoteInput = RefereeVoteInput(
    match = match,
    phaseName = PHASE_NAME,
    actor = VoteActor.Delegate(voterId),
    appearanceRating = 2,
    influenceRating = 3,
    teamworkRating = 4,
    comment = "Acceptable",
)

// ---------------------------------------------------------------------------
// in-memory stub repository
// ---------------------------------------------------------------------------

internal class InMemoryVoteRepository : VoteByRefereesRepository {
    private val store = mutableMapOf<String, VoteByReferees>()

    override fun findById(id: String): VoteByReferees? = store[id]
    override fun findAll(): List<VoteByReferees> = store.values.toList()
    override fun save(vote: VoteByReferees) { store[vote.id] = vote }
    override fun deleteById(id: String): Boolean = store.remove(id) != null
}

