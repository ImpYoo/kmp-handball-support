package de.exhumedo.kmp.handball_support.api.dto

import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.MatchDay
import de.exhumedo.kmp.handball_support.domain.model.Phase
import kotlinx.serialization.Serializable

@Serializable
data class PhaseResponseDto(
    val phaseId: Int,
    val tournamentId: Int,
    val seasonId: Int,
    val name: String,
    val shortName: String,
    val matchDays: List<MatchDayResponseDto>,
)

@Serializable
data class MatchDayResponseDto(
    val id: Int,
    val matches: List<MatchResponseDto>,
)

@Serializable
data class MatchResponseDto(
    val id: Int,
    val phaseId: Int,
    val timestamp: Long,
    val homeTeam: TeamDto,
    val awayTeam: TeamDto,
    val refereeA: MatchPersonDto?,
    val refereeB: MatchPersonDto?,
    val timekeeper: MatchPersonDto?,
    val scorekeeper: MatchPersonDto?,
    val delegate: MatchPersonDto?,
    val result: String,
    val halftimeResult: String,
    val hasVoteBy: HasVoteByDto,
)

@Serializable
data class TeamDto(val id: Int, val name: String)

@Serializable
data class MatchPersonDto(val id: Int, val name: String)

@Serializable
data class HasVoteByDto(val refereeTeam: Boolean, val delegate: Boolean)

fun Phase.toResponseDto() = PhaseResponseDto(
    phaseId = phaseId, tournamentId = tournamentId, seasonId = seasonId,
    name = name, shortName = shortName,
    matchDays = matchDays.map { it.toResponseDto() },
)

fun MatchDay.toResponseDto() = MatchDayResponseDto(
    id = id, matches = matches.map { it.toResponseDto() },
)

fun Match.toResponseDto() = MatchResponseDto(
    id = id, phaseId = phaseId, timestamp = timestamp,
    homeTeam = TeamDto(homeTeam.id, homeTeam.name),
    awayTeam = TeamDto(awayTeam.id, awayTeam.name),
    refereeA = refereeA?.let { MatchPersonDto(it.id, it.name) },
    refereeB = refereeB?.let { MatchPersonDto(it.id, it.name) },
    timekeeper = timekeeper?.let { MatchPersonDto(it.id, it.name) },
    scorekeeper = scorekeeper?.let { MatchPersonDto(it.id, it.name) },
    delegate = delegate?.let { MatchPersonDto(it.id, it.name) },
    result = result, halftimeResult = halftimeResult,
    hasVoteBy = HasVoteByDto(hasVoteBy.refereeTeam, hasVoteBy.delegate),
)
