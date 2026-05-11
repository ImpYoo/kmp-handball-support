package de.exhumedo.kmp.handball_support.domain.model

data class Team(val id: Int, val name: String)

data class Person(val id: Int, val name: String)

data class PhaseName(val fullName: String, val shortName: String)

/**
 * Tracks which actor roles have already submitted a vote for a match.
 *
 * @property refereeTeam true when the referee pair has submitted their joint vote.
 * @property delegate true when the delegate has submitted their vote.
 */
data class HasVoteBy(
    val refereeTeam: Boolean = false,
    val delegate: Boolean = false,
)

data class Match(
    val id: Int,
    val tournamentId: Int,
    val seasonId: Int,
    val phaseId: Int,
    val timestamp: Long,
    val homeTeam: Team,
    val awayTeam: Team,
    val refereeA: Person,
    val refereeB: Person,
    val timekeeper: Person,
    val scorekeeper: Person,
    val delegate: Person? = null,
    val result: String = "",
    val halftimeResult: String = "",
    val hasVoteBy: HasVoteBy = HasVoteBy(),
)

data class MatchDay(val id: Int, val matches: List<Match>)

data class Phase(
    val phaseId: Int,
    val tournamentId: Int,
    val seasonId: Int,
    val name: String,
    val shortName: String,
    val matchDays: List<MatchDay>,
)
