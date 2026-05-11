package de.exhumedo.kmp.handball_support.domain.usecase

import de.exhumedo.kmp.handball_support.domain.model.Match
import de.exhumedo.kmp.handball_support.domain.model.Phase

class GetPhasesWithMatchesByTimestampUseCase {
    operator fun invoke(
        phases: List<Phase>,
        fromTimestamp: Long,
        toTimestamp: Long = Long.MAX_VALUE,
    ): List<Phase> {
        return phases.mapNotNull { phase ->
            val filteredMatchDays = phase.matchDays
                .map { matchDay ->
                    matchDay.copy(matches = matchDay.matches.filter {
                        it.timestamp in fromTimestamp..<toTimestamp
                    })
                }
                .filter { it.matches.isNotEmpty() }

            if (filteredMatchDays.isEmpty()) null else phase.copy(matchDays = filteredMatchDays)
        }
    }
}

class GetMatchesOfPhaseByTimestampUseCase {
    operator fun invoke(
        phaseId: Int,
        phases: List<Phase>,
        fromTimestamp: Long,
        toTimestamp: Long = Long.MAX_VALUE,
    ): List<Match> {
        val phase = phases.firstOrNull { it.phaseId == phaseId } ?: return emptyList()
        return phase.matchDays.flatMap { it.matches }
            .filter { it.timestamp in fromTimestamp..<toTimestamp }
    }
}

class GetMatchOfPhaseUseCase {
    operator fun invoke(phaseId: Int, matchId: Int, phases: List<Phase>): Match? {
        return phases
            .firstOrNull { it.phaseId == phaseId }
            ?.matchDays
            ?.asSequence()
            ?.flatMap { it.matches.asSequence() }
            ?.firstOrNull { it.id == matchId }
    }
}

