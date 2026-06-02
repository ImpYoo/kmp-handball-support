package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.MatchDay
import de.exhumedo.kmp.handball_support.domain.model.Person
import de.exhumedo.kmp.handball_support.domain.model.Phase
import de.exhumedo.kmp.handball_support.domain.model.Team
import de.exhumedo.kmp.handball_support.domain.repository.PhaseRepository
import de.exhumedo.kmp.handball_support.sportradar.error.SportradarResult
import de.exhumedo.kmp.handball_support.sportradar.repository.SportradarPhaseRepository
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import de.exhumedo.kmp.handball_support.sportradar.model.Match as SrMatch
import de.exhumedo.kmp.handball_support.sportradar.model.MatchDay as SrMatchDay
import de.exhumedo.kmp.handball_support.sportradar.model.Phase as SrPhase

/**
 * Adapts [SportradarPhaseRepository] to the synchronous domain [PhaseRepository].
 *
 * Officials (refereeA/B, timekeeper, scorekeeper, delegate) may be missing in the upstream feed;
 * they are surfaced as `null` to the API layer and only the vote-submission use cases reject
 * matches with incomplete officials. A Sportradar fetch failure returns an empty list.
 */
class SportradarPhaseRepositoryAdapter(
    private val sportradarRepository: SportradarPhaseRepository,
) : PhaseRepository {

    private val log = LoggerFactory.getLogger(SportradarPhaseRepositoryAdapter::class.java)

    override fun getAllPhases(): List<Phase> {
        val result = runBlocking { sportradarRepository.getAllPhases() }
        return when (result) {
            is SportradarResult.Success -> result.value.map { it.toDomain() }
            is SportradarResult.Failure -> {
                log.error("Failed to load phases from Sportradar: {}", result.error)
                emptyList()
            }
        }
    }

    private fun SrPhase.toDomain(): Phase =
        Phase(
            phaseId      = phaseId,
            tournamentId = tournamentId,
            seasonId     = seasonId,
            name         = name,
            shortName    = shortName,
            matchDays    = matchDays.map { it.toDomain(phaseId) },
        )

    private fun SrMatchDay.toDomain(phaseId: Int): MatchDay =
        MatchDay(id = id, matches = matches.map { it.toDomain(phaseId) })

    private fun SrMatch.toDomain(phaseId: Int): Match =
        Match(
            id             = id,
            tournamentId   = tournamentId,
            seasonId       = seasonId,
            phaseId        = phaseId,
            timestamp      = timestamp,
            homeTeam       = Team(homeTeam.id, homeTeam.name),
            awayTeam       = Team(awayTeam.id, awayTeam.name),
            refereeA       = refereeA?.let { Person(it.id, it.name) },
            refereeB       = refereeB?.let { Person(it.id, it.name) },
            timekeeper     = timekeeper?.let { Person(it.id, it.name) },
            scorekeeper    = scorekeeper?.let { Person(it.id, it.name) },
            delegate       = delegate?.let { Person(it.id, it.name) },
            result         = result,
            halftimeResult = halftimeResult,
        )
}
