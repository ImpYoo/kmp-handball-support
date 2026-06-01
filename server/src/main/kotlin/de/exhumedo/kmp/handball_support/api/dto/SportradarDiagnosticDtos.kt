package de.exhumedo.kmp.handball_support.api.dto

import de.exhumedo.kmp.handball_support.sportradar.error.SportradarError
import de.exhumedo.kmp.handball_support.sportradar.model.Match
import de.exhumedo.kmp.handball_support.sportradar.model.MatchDay
import de.exhumedo.kmp.handball_support.sportradar.model.Phase
import de.exhumedo.kmp.handball_support.sportradar.model.PhaseRef
import de.exhumedo.kmp.handball_support.sportradar.model.SeasonRef
import de.exhumedo.kmp.handball_support.sportradar.model.TournamentRef
import kotlinx.serialization.Serializable

// ── Sportradar diagnostic response DTOs ───────────────────────────────────────
// Used only by the internal /api/sportradar/** routes for HTTP-client testing.
// ─────────────────────────────────────────────────────────────────────────────

// ── Phase / match DTOs ────────────────────────────────────────────────────────

@Serializable
data class SportradarPhaseSummaryDto(
    val phaseId: Int,
    val tournamentId: Int,
    val seasonId: Int,
    val name: String,
    val shortName: String,
    val matchDayCount: Int,
    val matchCount: Int,
)

@Serializable
data class SportradarPhaseDetailDto(
    val phaseId: Int,
    val tournamentId: Int,
    val seasonId: Int,
    val name: String,
    val shortName: String,
    val matchDays: List<SportradarMatchDayDto>,
)

@Serializable
data class SportradarMatchDayDto(
    val matchDayId: Int,
    val matchCount: Int,
    val matches: List<SportradarMatchDetailDto>,
)

@Serializable
data class SportradarMatchDetailDto(
    val matchId: Int,
    val matchDayId: Int,
    val timestamp: Long,
    val homeTeam: SportradarTeamDto,
    val awayTeam: SportradarTeamDto,
    val result: String,
    val halftimeResult: String,
    val refereeA: SportradarPersonDto?,
    val refereeB: SportradarPersonDto?,
    val timekeeper: SportradarPersonDto?,
    val scorekeeper: SportradarPersonDto?,
    val delegate: SportradarPersonDto?,
)

@Serializable
data class SportradarTeamDto(val id: Int, val name: String)

@Serializable
data class SportradarPersonDto(val id: Int, val name: String)

// ── Error DTO ─────────────────────────────────────────────────────────────────

@Serializable
data class SportradarFetchErrorDto(
    val error: String,
    val detail: String? = null,
)

@Serializable
data class SportradarRefreshDto(
    val tournamentCount: Int,
    val configuredSeasonCount: Int,
    val phaseCount: Int,
    val refreshedAtEpochMillis: Long,
)

// ── Tournament / season DTOs (from Sportradar /tournament feed) ───────────────

@Serializable
data class SportradarPhaseRefDto(
    val id: Int,
    val name: String,
    val startDate: String,
    val endDate: String,
)

@Serializable
data class SportradarSeasonDto(
    val id: Int,
    val name: String,
    val year: String,
    val status: String,
    val phaseCount: Int,
    val phases: List<SportradarPhaseRefDto>,
)

@Serializable
data class SportradarTournamentDto(
    val id: Int,
    val name: String,
    val seasonCount: Int,
    val seasons: List<SportradarSeasonDto>,
)

/** Season detail: combines tournament-feed metadata with fixture-feed phase summaries. */
@Serializable
data class SportradarSeasonDetailDto(
    val tournamentId: Int,
    val seasonId: Int,
    val name: String,
    val year: String,
    val status: String,
    val phases: List<SportradarPhaseSummaryDto>,
)

// ── Mapping helpers ───────────────────────────────────────────────────────────
// Note: all helpers use distinct names to avoid overload-resolution ambiguity
// when Kotlin infers the receiver type inside higher-order functions.

fun Phase.toSummaryDto() = SportradarPhaseSummaryDto(
    phaseId       = phaseId,
    tournamentId  = tournamentId,
    seasonId      = seasonId,
    name          = name,
    shortName     = shortName,
    matchDayCount = matchDays.size,
    matchCount    = matchDays.sumOf { it.matches.size },
)

fun Phase.toDetailDto() = SportradarPhaseDetailDto(
    phaseId      = phaseId,
    tournamentId = tournamentId,
    seasonId     = seasonId,
    name         = name,
    shortName    = shortName,
    matchDays    = matchDays.map { it.toMatchDayDto() },
)

fun MatchDay.toMatchDayDto() = SportradarMatchDayDto(
    matchDayId = id,
    matchCount = matches.size,
    matches    = matches.map { it.toMatchDetailDto(id) },
)

fun Match.toMatchDetailDto(matchDayId: Int) = SportradarMatchDetailDto(
    matchId        = id,
    matchDayId     = matchDayId,
    timestamp      = timestamp,
    homeTeam       = SportradarTeamDto(homeTeam.id, homeTeam.name),
    awayTeam       = SportradarTeamDto(awayTeam.id, awayTeam.name),
    result         = result,
    halftimeResult = halftimeResult,
    refereeA       = refereeA?.let { SportradarPersonDto(it.id, it.name) },
    refereeB       = refereeB?.let { SportradarPersonDto(it.id, it.name) },
    timekeeper     = timekeeper?.let { SportradarPersonDto(it.id, it.name) },
    scorekeeper    = scorekeeper?.let { SportradarPersonDto(it.id, it.name) },
    delegate       = delegate?.let { SportradarPersonDto(it.id, it.name) },
)

fun SportradarError.toErrorDto(): SportradarFetchErrorDto = when (this) {
    is SportradarError.EmptyResponse -> SportradarFetchErrorDto("EMPTY_RESPONSE")
    is SportradarError.HttpError     -> SportradarFetchErrorDto("HTTP_ERROR", "status=$statusCode")
    is SportradarError.NetworkError  -> SportradarFetchErrorDto("NETWORK_ERROR", "upstream request failed")
    is SportradarError.ParseError    -> SportradarFetchErrorDto("PARSE_ERROR", "upstream response could not be parsed")
}

fun PhaseRef.toRefDto() = SportradarPhaseRefDto(
    id = id, name = name, startDate = startDate, endDate = endDate,
)

fun SeasonRef.toSeasonDto() = SportradarSeasonDto(
    id         = id,
    name       = name,
    year       = year,
    status     = status,
    phaseCount = phases.size,
    phases     = phases.map { it.toRefDto() },
)

fun TournamentRef.toTournamentDto() = SportradarTournamentDto(
    id          = id,
    name        = name,
    seasonCount = seasons.size,
    seasons     = seasons.map { it.toSeasonDto() },
)
