package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.MatchDay
import de.exhumedo.kmp.handball_support.domain.model.Person
import de.exhumedo.kmp.handball_support.domain.model.Phase
import de.exhumedo.kmp.handball_support.domain.model.Team
import java.time.Instant
import java.time.ZoneOffset
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

    private val now = Instant.now().atOffset(ZoneOffset.UTC).toLocalDate()
    private fun weekStart(): Long = now.minusDays(now.dayOfWeek.value.toLong() - 1L)
        .atStartOfDay(ZoneOffset.UTC)
        .toEpochSecond()

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
    private fun offset(days: Int, hours: Int, minutes: Int): Long =
        weekStart() + days * 86_400L + hours * 3_600L + minutes * 60L

    // ── Seed matches ─────────────────────────────────────────────────────────

    // Matchday 30 — two days ago
    private val md30 = MatchDay(
        id = 30,
        matches = listOf(
            Match(
                id = 3001, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1786226465L,
                homeTeam = Team(1, "THW Kiel"),
                awayTeam = Team(2, "SG Flensburg-Handewitt"),
                refereeA = ref(101, "Thomas Müller"),
                refereeB = ref(102, "Stefan Schulz"),
                timekeeper  = tk(301, "Hans Koch"),
                scorekeeper = sk(401, "Maria Braun"),
                delegate    = del(201, "Gerhard Meier"),
                result = "31:28", halftimeResult = "16:13",
            ),
            Match(
                id = 3002, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1786233665L,
                homeTeam = Team(4, "SC Magdeburg"),
                awayTeam = Team(3, "Rhein-Neckar Löwen"),
                refereeA = ref(103, "Thomas Müller"),
                refereeB = ref(104, "Stefan Schulz"),
                timekeeper  = tk(302, "Hans Koch"),
                scorekeeper = sk(402, "Maria Braun"),
                delegate    = del(202, "Gerhard Meier"),
                result = "29:26", halftimeResult = "14:12",
            ),
        ),
    )

    // Matchday 31 — yesterday
    private val md31 = MatchDay(
        id = 31,
        matches = listOf(
            Match(
                id = 3101, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1786318265L,
                homeTeam = Team(5, "Füchse Berlin"),
                awayTeam = Team(6, "MT Melsungen"),
                refereeA = ref(105, "Thomas Müller"),
                refereeB = ref(106, "Stefan Schulz"),
                timekeeper  = tk(303, "Hans Koch"),
                scorekeeper = sk(403, "Maria Braun"),
                delegate    = del(203, "Gerhard Meier"),
                result = "25:27", halftimeResult = "13:14",
            ),
            Match(
                id = 3102, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1786325465L,
                homeTeam = Team(8, "HSV Hamburg"),
                awayTeam = Team(7, "TSV Hannover-Burgdorf"),
                refereeA = ref(107, "Thomas Müller"),
                refereeB = ref(108, "Stefan Schulz"),
                timekeeper  = tk(304, "Hans Koch"),
                scorekeeper = sk(404, "Maria Braun"),
                delegate    = null,
                result = "28:24", halftimeResult = "15:11",
            ),
        ),
    )

    // Matchday 32 — today/upcoming
    private val md32 = MatchDay(
        id = 32,
        matches = listOf(
            Match(
                id = 3201, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1786393865L,
                homeTeam = Team(2, "SG Flensburg-Handewitt"),
                awayTeam = Team(4, "SC Magdeburg"),
                refereeA = ref(101, "Thomas Müller"),
                refereeB = ref(103, "Stefan Schulz"),
                timekeeper  = tk(305, "Hans Koch"),
                scorekeeper = sk(405, "Maria Braun"),
                delegate    = del(204, "Gerhard Meier"),
                 
            ),
            Match(
                id = 3202, tournamentId = 1, seasonId = 1, phaseId = 1,
                timestamp = 1786401065L,
                homeTeam = Team(1, "THW Kiel"),
                awayTeam = Team(8, "HSV Hamburg"),
                refereeA = ref(105, "Thomas Müller"),
                refereeB = ref(107, "Stefan Schulz"),
                timekeeper  = tk(306, "Hans Koch"),
                scorekeeper = sk(406, "Maria Braun"),
                delegate    = del(201, "Gerhard Meier"),
                 
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
