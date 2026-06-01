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
 * Adapts [SportradarPhaseRepository] (async Sportradar model) to the synchronous
 * domain [PhaseRepository] interface consumed by MatchApplicationService.
 *
 * Matches missing required officials (refereeA/B, timekeeper, scorekeeper)
 * are skipped with WARN. A Sportradar fetch failure returns empty list (server stays UP).
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
        MatchDay(id = id, matches = matches.mapNotNull { it.toDomain(phaseId) })

    private fun SrMatch.toDomain(phaseId: Int): Match? {
        val rA = refereeA
        val rB = refereeB
        val tk = timekeeper
        val sk = scorekeeper
        if (rA == null || rB == null || tk == null || sk == null) {
            log.warn(
                "Skipping match id={} phaseId={}: missing rA={} rB={} tk={} sk={}",
                id, phaseId,
                if (rA == null) "MISSING" else "ok",
                if (rB == null) "MISSING" else "ok",
                if (tk == null) "MISSING" else "ok",
                if (sk == null) "MISSING" else "ok",
            )
            return null
        }
        return Match(
            id             = id,
            tournamentId   = tournamentId,
            seasonId       = seasonId,
            phaseId        = phaseId,
            timestamp      = timestamp,
            homeTeam       = Team(homeTeam.id, homeTeam.name),
            awayTeam       = Team(awayTeam.id, awayTeam.name),
            refereeA       = Person(rA.id, rA.name),
            refereeB       = Person(rB.id, rB.name),
            timekeeper     = Person(tk.id, tk.name),
            scorekeeper    = Person(sk.id, sk.name),
            delegate       = delegate?.let { Person(it.id, it.name) },
            result         = result,
            halftimeResult = halftimeResult,
        )
    }
}
