package de.exhumedo.kmp.handball_support.sportradar.mapper

import de.exhumedo.kmp.handball_support.sportradar.model.Match
import de.exhumedo.kmp.handball_support.sportradar.model.MatchDay
import de.exhumedo.kmp.handball_support.sportradar.model.Person
import de.exhumedo.kmp.handball_support.sportradar.model.Phase
import de.exhumedo.kmp.handball_support.sportradar.model.Team
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarFixturesResponse
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarMatch
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarOfficial
import de.exhumedo.kmp.handball_support.sportradar.config.TournamentConfig
import org.slf4j.LoggerFactory

// ── Sportradar DTO → domain model mapper ─────────────────────────────────────
// Pure functions — no side effects, no I/O. Safe to test independently.
// ─────────────────────────────────────────────────────────────────────────────

internal object SportradarMapper {

    private val log = LoggerFactory.getLogger(SportradarMapper::class.java)

    internal fun toPhases(response: SportradarFixturesResponse, config: TournamentConfig): List<Phase> {
        val tournament = response.doc.firstOrNull()?.data?.tournament ?: return emptyList()

        return tournament.phases.map { phase ->
            val cleanedName = phase.name.cleanSeasonSuffix()
            Phase(
                phaseId = phase.id,
                tournamentId = config.tournamentId,
                seasonId = config.seasonId,
                name = cleanedName,
                shortName = cleanedName.toPhaseShortName(),
                matchDays = phase.matchdays.map { matchDay ->
                    MatchDay(
                        id = matchDay.id,
                        matches = matchDay.matches.mapNotNull { match ->
                            toMatch(match, phase.id, config)
                        },
                    )
                },
            )
        }
    }

    private fun toMatch(
        match: SportradarMatch,
        phaseId: Int,
        config: TournamentConfig,
    ): Match? = try {
        val officials = match.referees.associateBy { it.typeKey }
        Match(
            id = match.id,
            tournamentId = config.tournamentId,
            seasonId = config.seasonId,
            phaseId = phaseId,
            timestamp = match.playDate.uts,   // Sportradar uts is already Unix epoch seconds
            homeTeam = match.homeTeam.let { Team(it.id, it.name.cleanTeamName()) },
            awayTeam = match.awayTeam.let { Team(it.id, it.name.cleanTeamName()) },
            refereeA = officials["first_referee"]?.toPerson(),
            refereeB = officials["second_referee"]?.toPerson(),
            timekeeper = officials["timekeeper"]?.toPerson(),
            scorekeeper = officials["secretary"]?.toPerson(),
            delegate = officials["delegate"]?.toPerson(),
            result = buildResult(match.homeTeam.finalResult, match.awayTeam.finalResult),
            halftimeResult = buildResult(match.homeTeam.resultFirstHalf, match.awayTeam.resultFirstHalf),
        )
    } catch (e: Exception) {
            // A single malformed match must not abort the entire matchday — log and skip.
            log.warn("Skipping match id={} in phase={}: {}", match.id, phaseId, e.message)
            null
        }

    private fun SportradarOfficial.toPerson(): Person = Person(id = id, name = name)

    private fun buildResult(home: String, away: String): String =
        if (home.isNotBlank() && away.isNotBlank()) "$home : $away" else ""
}

/** Remove league-name prefixes Sportradar bakes into team names. */
private val TEAM_NAME_PREFIXES = listOf(
    "3. Liga Frauen",
    "3. Liga Maenner",
    "3. Liga Männer",
    "JBLH männlich",
    "JBLH weiblich",
    " JBLH A-Jugend männlich",
    " JBLH B-Jugend männlich",
    " JBLH A-Jugend weiblich",
    " JBLH B-Jugend weiblich",
)

private fun String.cleanTeamName(): String =
    TEAM_NAME_PREFIXES.fold(this) { acc, prefix -> acc.replace(prefix, "") }.trim()


