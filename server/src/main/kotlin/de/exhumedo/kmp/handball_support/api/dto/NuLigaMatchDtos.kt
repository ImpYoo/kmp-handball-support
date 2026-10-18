package de.exhumedo.kmp.handball_support.api.dto

import de.exhumedo.kmp.handball_support.nuliga.NuLigaClient
import de.exhumedo.kmp.handball_support.nuliga.NuLigaMatchPerson
import de.exhumedo.kmp.handball_support.nuliga.NuLigaOfficial
import de.exhumedo.kmp.handball_support.nuliga.NuLigaPlayer
import de.exhumedo.kmp.handball_support.nuliga.NuLigaResolvedMatch
import de.exhumedo.kmp.handball_support.nuliga.NuLigaResolvedTeam
import kotlinx.serialization.Serializable

/**
 * `POST /api/nuliga/match-resolutions` — an immutable resolution attempt. The server stores
 * nothing (stateless), so a resolution is a transient resource: the client posts the source
 * URL and receives the match as it stands right now, rosters included.
 *
 * Timestamps follow the rest of this API (epoch milliseconds, e.g. MatchResponseDto.timestamp).
 */
@Serializable
data class NuLigaMatchResolutionRequest(
    val url: String,
)

@Serializable
data class NuLigaMatchResolutionResponse(
    val sourceId: String = NuLigaClient.SOURCE_ID,
    /** Requested URL, echoed back so a client can correlate concurrent requests. */
    val sourceUrl: String,
    /** Canonical meeting id at nuLiga. */
    val sourceMatchId: String,
    val resolvedAt: Long,
    val match: NuLigaMatchResponse,
)

@Serializable
data class NuLigaMatchResponse(
    val id: String,
    /** Kick-off as epoch milliseconds, when nuLiga publishes one. */
    val scheduledAt: Long? = null,
    val status: NuLigaStatusResponse,
    val config: NuLigaConfigResponse,
    val competition: NuLigaCompetitionResponse? = null,
    val venue: NuLigaVenueResponse? = null,
    val home: NuLigaTeamResponse,
    val away: NuLigaTeamResponse,
    val referees: List<NuLigaPersonResponse>,
    val tableOfficials: List<NuLigaPersonResponse>,
    val supervisors: List<NuLigaPersonResponse>,
)

@Serializable
data class NuLigaStatusResponse(
    val id: String,
    val name: String,
    val isLive: Boolean,
    val isFinished: Boolean = false,
)

@Serializable
data class NuLigaConfigResponse(
    val periods: Int,
    val minutesPerPeriod: Int,
)

@Serializable
data class NuLigaCompetitionResponse(val name: String)

@Serializable
data class NuLigaVenueResponse(val name: String)

@Serializable
data class NuLigaTeamResponse(
    val name: String,
    val logoUrl: String? = null,
    val players: List<NuLigaPlayerResponse>,
    val officials: List<NuLigaOfficialResponse>,
)

@Serializable
data class NuLigaPlayerResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val number: Int? = null,
)

@Serializable
data class NuLigaOfficialResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    /** IHF designation A–E. */
    val badge: String? = null,
)

@Serializable
data class NuLigaPersonResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    /** Normalized role code, UPPER_SNAKE_CASE (REFEREE, TIMEKEEPER, SECRETARY, …). */
    val roleCode: String,
    /** 1-based within the role list. */
    val position: Int,
)

fun NuLigaResolvedMatch.toResponseDto(
    sourceUrl: String,
    resolvedAtEpochMillis: Long,
) = NuLigaMatchResolutionResponse(
    sourceUrl = sourceUrl,
    sourceMatchId = sourceMatchId,
    resolvedAt = resolvedAtEpochMillis,
    match = NuLigaMatchResponse(
        id = sourceMatchId,
        scheduledAt = scheduledAt?.toEpochMilli(),
        status = NuLigaStatusResponse(
            id = if (isLive) "LIVE" else "SCHEDULED",
            name = if (isLive) "Läuft" else "Angesetzt",
            isLive = isLive,
        ),
        config = NuLigaConfigResponse(periods = periods, minutesPerPeriod = minutesPerPeriod),
        competition = competitionName?.let { NuLigaCompetitionResponse(name = it) },
        venue = venueName?.let { NuLigaVenueResponse(name = it) },
        home = home.toResponseDto(),
        away = away.toResponseDto(),
        referees = referees.map { it.toResponseDto() },
        tableOfficials = tableOfficials.map { it.toResponseDto() },
        supervisors = supervisors.map { it.toResponseDto() },
    ),
)

private fun NuLigaResolvedTeam.toResponseDto() = NuLigaTeamResponse(
    name = name,
    logoUrl = logoUrl,
    players = players.map { it.toResponseDto() },
    officials = officials.map { it.toResponseDto() },
)

private fun NuLigaPlayer.toResponseDto() = NuLigaPlayerResponse(
    id = id, firstName = firstName, lastName = lastName, number = number,
)

private fun NuLigaOfficial.toResponseDto() = NuLigaOfficialResponse(
    id = id, firstName = firstName, lastName = lastName, badge = badge,
)

private fun NuLigaMatchPerson.toResponseDto() = NuLigaPersonResponse(
    id = id, firstName = firstName, lastName = lastName,
    roleCode = roleCode, position = position,
)