package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.MatchDay
import de.exhumedo.kmp.handball_support.domain.model.Person
import de.exhumedo.kmp.handball_support.domain.model.Phase
import de.exhumedo.kmp.handball_support.domain.model.Team
import de.exhumedo.kmp.handball_support.domain.repository.PhaseRepository

/**
 * In-memory mock implementation of [PhaseRepository].
 *
 * Seed data represents one Handball Bundesliga phase with three match days:
 *  - Matchday 30 and 31 in the past (timestamps before May 11 2026)
 *  - Matchday 32 in the future
 *
 * All person IDs follow these ID ranges so they map cleanly to real evaluations:
 *  - Referees:     100–199
 *  - Delegates:    200–299
 *  - Timekeepers:  300–399
 *  - Scorekeepers: 400–499
 *
 * Replace this class with a real adapter (database, external API) when ready.
 */
class MockPhaseRepository : PhaseRepository {

    // ── Master data ──────────────────────────────────────────────────────────

    private val kiel         = Team(1, "THW Kiel")
    private val flensburg    = Team(2, "SG Flensburg-Handewitt")
    private val lwn          = Team(3, "Rhein-Neckar Löwen")
    private val magdeburg    = Team(4, "SC Magdeburg")
    private val fuchse       = Team(5, "Füchse Berlin")
    private val melsungen    = Team(6, "MT Melsungen")
    private val hannover     = Team(7, "TSV Hannover-Burgdorf")
    private val hamburg      = Team(8, "HSV Hamburg")

    private fun ref(id: Int, name: String)  = Person(id, name)
    private fun del(id: Int, name: String)  = Person(id, name)
    private fun tk(id: Int, name: String)   = Person(id, name)
    private fun sk(id: Int, name: String)   = Person(id, name)

    // ── Seed matches ─────────────────────────────────────────────────────────

    // Matchday 30 — May 3, 2026 (past)
    private val md30 = MatchDay(
        id = 30,
        matches = listOf(
            Match(
                id = 3001, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1_746_286_200L, // May 3, 2026 15:30 UTC
                homeTeam = kiel, awayTeam = flensburg,
                refereeA = ref(101, "Thomas Müller"),
                refereeB = ref(102, "Stefan Schulz"),
                timekeeper  = tk(301, "Hans Koch"),
                scorekeeper = sk(401, "Maria Braun"),
                delegate    = del(201, "Werner Zimmermann"),
                result = "31:28", halftimeResult = "16:13",
            ),
            Match(
                id = 3002, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1_746_293_400L, // May 3, 2026 17:30 UTC
                homeTeam = magdeburg, awayTeam = lwn,
                refereeA = ref(103, "Klaus Weber"),
                refereeB = ref(104, "Andreas Richter"),
                timekeeper  = tk(302, "Petra Hoffmann"),
                scorekeeper = sk(402, "Frank Becker"),
                delegate    = del(202, "Ute Schäfer"),
                result = "29:26", halftimeResult = "14:12",
            ),
        ),
    )

    // Matchday 31 — May 9, 2026 (past)
    private val md31 = MatchDay(
        id = 31,
        matches = listOf(
            Match(
                id = 3101, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1_746_810_000L, // May 9, 2026 17:00 UTC
                homeTeam = fuchse, awayTeam = melsungen,
                refereeA = ref(105, "Jürgen Krause"),
                refereeB = ref(106, "Markus Vogel"),
                timekeeper  = tk(303, "Angelika Wimmer"),
                scorekeeper = sk(403, "Bernd Neumann"),
                delegate    = del(203, "Rolf Lange"),
                result = "25:27", halftimeResult = "13:14",
            ),
            Match(
                id = 3102, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1_746_817_200L, // May 9, 2026 19:00 UTC
                homeTeam = hamburg, awayTeam = hannover,
                refereeA = ref(107, "Dieter Haas"),
                refereeB = ref(108, "Sabine Fischer"),
                timekeeper  = tk(304, "Klaus-Peter Stein"),
                scorekeeper = sk(404, "Renate Wolf"),
                delegate    = null, // no delegate for this match
                result = "28:24", halftimeResult = "15:11",
            ),
        ),
    )

    // Matchday 32 — May 17, 2026 (upcoming)
    private val md32 = MatchDay(
        id = 32,
        matches = listOf(
            Match(
                id = 3201, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1_747_490_400L, // May 17, 2026 14:00 UTC
                homeTeam = flensburg, awayTeam = magdeburg,
                refereeA = ref(101, "Thomas Müller"),
                refereeB = ref(103, "Klaus Weber"),
                timekeeper  = tk(305, "Holger Krüger"),
                scorekeeper = sk(405, "Monika Sauer"),
                delegate    = del(204, "Gerhard Meier"),
            ),
            Match(
                id = 3202, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1_747_497_600L, // May 17, 2026 16:00 UTC
                homeTeam = kiel, awayTeam = hamburg,
                refereeA = ref(105, "Jürgen Krause"),
                refereeB = ref(107, "Dieter Haas"),
                timekeeper  = tk(306, "Susanne Arnold"),
                scorekeeper = sk(406, "Manfred Werner"),
                delegate    = del(201, "Werner Zimmermann"),
            ),
        ),
    )

    private val phase = Phase(
        phaseId      = 1,
        tournamentId = 1,
        seasonId     = 1,
        name         = "Handball Bundesliga 2025/26",
        shortName    = "HBL",
        matchDays    = listOf(md30, md31, md32),
    )

    override fun getAllPhases(): List<Phase> = listOf(phase)
}

