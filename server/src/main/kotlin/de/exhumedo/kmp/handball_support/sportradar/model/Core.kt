package de.exhumedo.kmp.handball_support.sportradar.model

// ── Core domain models ────────────────────────────────────────────────────────
// KMP-ready: no JVM-specific imports.
// ─────────────────────────────────────────────────────────────────────────────

data class Person(
    val id: Int,
    val name: String,
)

data class Team(
    val id: Int,
    val name: String,
)

data class PhaseName(
    val fullName: String,
    val shortName: String,
)

data class Match(
    val id: Int,
    val tournamentId: Int,
    val seasonId: Int,
    val phaseId: Int,
    /** Unix epoch milliseconds */
    val timestamp: Long,
    val homeTeam: Team,
    val awayTeam: Team,
    val refereeA: Person?,
    val refereeB: Person?,
    val timekeeper: Person?,
    val scorekeeper: Person?,
    val delegate: Person?,
    val result: String = "",
    val halftimeResult: String = "",
)

data class MatchDay(
    val id: Int,
    val matches: List<Match>,
)

data class Phase(
    val phaseId: Int,
    val tournamentId: Int,
    val seasonId: Int,
    val name: String,
    val shortName: String,
    val matchDays: List<MatchDay>,
)

// ── Tournament list model (from /tournament feed) ─────────────────────────────

data class PhaseRef(
    val id: Int,
    val name: String,
    val startDate: String,
    val endDate: String,
)

data class SeasonRef(
    val id: Int,
    val name: String,
    val year: String,
    val status: String,
    val phases: List<PhaseRef>,
)

data class TournamentRef(
    val id: Int,
    val name: String,
    val seasons: List<SeasonRef>,
)

