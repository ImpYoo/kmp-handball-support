 package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryAttachment
import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryEntry
import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryPresenter
import de.exhumedo.kmp.handball_support.coaching.HistoryEventType
import de.exhumedo.kmp.handball_support.coaching.RefereeCoachingPresenter
import de.exhumedo.kmp.handball_support.matchconsole.MatchSetupPresenter
import de.exhumedo.kmp.handball_support.matchconsole.Player
import de.exhumedo.kmp.handball_support.matchconsole.RosterPresenter
import de.exhumedo.kmp.handball_support.matchconsole.RosterTeam
import de.exhumedo.kmp.handball_support.matchconsole.ScoreboardPresenter
import de.exhumedo.kmp.handball_support.matchconsole.StopwatchPresenter
import kotlin.math.abs

/**
 * Maps the coaching-session presenters to/from a [PersistedSession].
 *
 * [capture] reads the presenters' current state; [restore] writes a persisted
 * snapshot back into them. The coaching sheet is restored by replaying
 * select/deselect through the presenter so domain scoring stays authoritative.
 */
object SessionMapper {

    fun capture(
        coaching: RefereeCoachingPresenter,
        history: CoachingHistoryPresenter,
        stopwatch: StopwatchPresenter,
        scoreboard: ScoreboardPresenter,
        roster: RosterPresenter,
        matchSetup: MatchSetupPresenter,
    ): PersistedSession = PersistedSession(
        matchSetup = PersistedMatchSetup(
            homeTeamName = matchSetup.homeTeamName,
            homeTeamAbbreviation = matchSetup.homeTeamAbbreviation,
            guestTeamName = matchSetup.guestTeamName,
            guestTeamAbbreviation = matchSetup.guestTeamAbbreviation,
            firstRefereeName = matchSetup.firstRefereeName,
            secondRefereeName = matchSetup.secondRefereeName,
        ),
        homePlayers = roster.homePlayers.map { it.toPersisted() },
        guestPlayers = roster.guestPlayers.map { it.toPersisted() },
        homeScore = scoreboard.homeScore,
        guestScore = scoreboard.guestScore,
        stopwatchElapsedMillis = stopwatch.elapsedMillis,
        history = history.entries.map { it.toPersisted() },
        criterionCounts = coaching.criteria.flatMap { criterion ->
            criterion.defectGroups.flatMap { group ->
                group.rootCauses
                    .filter { it.count != 0 }
                    .map { cause ->
                        PersistedRootCauseCount(
                            criterionId = criterion.id,
                            groupId = group.id,
                            rootCauseId = cause.id,
                            count = cause.count,
                        )
                    }
            }
        },
    )

    fun restore(
        session: PersistedSession,
        coaching: RefereeCoachingPresenter,
        history: CoachingHistoryPresenter,
        stopwatch: StopwatchPresenter,
        scoreboard: ScoreboardPresenter,
        roster: RosterPresenter,
        matchSetup: MatchSetupPresenter,
    ) {
        matchSetup.homeTeamName = session.matchSetup.homeTeamName
        matchSetup.homeTeamAbbreviation = session.matchSetup.homeTeamAbbreviation
        matchSetup.guestTeamName = session.matchSetup.guestTeamName
        matchSetup.guestTeamAbbreviation = session.matchSetup.guestTeamAbbreviation
        matchSetup.firstRefereeName = session.matchSetup.firstRefereeName
        matchSetup.secondRefereeName = session.matchSetup.secondRefereeName

        roster.restore(
            home = session.homePlayers.map { it.toPlayer() },
            guest = session.guestPlayers.map { it.toPlayer() },
        )

        scoreboard.restore(session.homeScore, session.guestScore)
        stopwatch.setElapsed(session.stopwatchElapsedMillis)

        // Rebuild the coaching sheet by replaying counts through the domain.
        coaching.reset()
        session.criterionCounts.forEach { c ->
            repeat(abs(c.count)) {
                if (c.count > 0) {
                    coaching.select(c.criterionId, c.groupId, c.rootCauseId)
                } else {
                    coaching.deselect(c.criterionId, c.groupId, c.rootCauseId)
                }
            }
        }

        history.restore(session.history.map { it.toEntry() })
    }
}

private fun Player.toPersisted() = PersistedPlayer(id = id, number = number, name = name)
private fun PersistedPlayer.toPlayer() = Player(id = id, number = number, name = name)

private fun CoachingHistoryEntry.toPersisted() = PersistedHistoryEntry(
    id = id,
    gameTimeMillis = gameTimeMillis,
    homeScore = homeScore,
    guestScore = guestScore,
    type = type.name,
    criterionId = criterionId,
    defectGroupId = defectGroupId,
    rootCauseId = rootCauseId,
    goalTeam = goalTeam?.name,
    selected = selected,
    attachment = attachment?.let {
        PersistedAttachment(
            team = it.team?.name,
            teamLabel = it.teamLabel,
            playerId = it.playerId,
            playerLabel = it.playerLabel,
            refereeName = it.refereeName,
        )
    },
    note = note,
)

private fun PersistedHistoryEntry.toEntry() = CoachingHistoryEntry(
    id = id,
    gameTimeMillis = gameTimeMillis,
    homeScore = homeScore,
    guestScore = guestScore,
    type = runCatching { HistoryEventType.valueOf(type) }.getOrDefault(HistoryEventType.ROOT_CAUSE),
    criterionId = criterionId,
    defectGroupId = defectGroupId,
    rootCauseId = rootCauseId,
    goalTeam = goalTeam?.let { name -> runCatching { RosterTeam.valueOf(name) }.getOrNull() },
    selected = selected,
    attachment = attachment?.let {
        CoachingHistoryAttachment(
            team = it.team?.let { name -> runCatching { RosterTeam.valueOf(name) }.getOrNull() },
            teamLabel = it.teamLabel,
            playerId = it.playerId,
            playerLabel = it.playerLabel,
            refereeName = it.refereeName,
        )
    },
    note = note,
)

