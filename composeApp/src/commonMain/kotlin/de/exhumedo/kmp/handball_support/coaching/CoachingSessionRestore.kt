package de.exhumedo.kmp.handball_support.coaching

import de.exhumedo.kmp.handball_support.client.CoachingHistoryEntryDto
import de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto
import de.exhumedo.kmp.handball_support.matchconsole.MatchSetupPresenter
import de.exhumedo.kmp.handball_support.matchconsole.Player
import de.exhumedo.kmp.handball_support.matchconsole.RosterPresenter
import de.exhumedo.kmp.handball_support.matchconsole.RosterTeam
import de.exhumedo.kmp.handball_support.matchconsole.ScoreboardPresenter
import de.exhumedo.kmp.handball_support.matchconsole.StopwatchPresenter
import de.exhumedo.kmp.handball_support.ui.restoreCoachingFromReport

/**
 * Restores the whole live coaching session from a saved report so that
 * "Fortsetzen" resumes exactly where the user left off.
 */
fun restoreCoachingSessionFromReport(
    report: CoachingReportResponseDto,
    coaching: RefereeCoachingPresenter,
    matchSetup: MatchSetupPresenter,
    history: CoachingHistoryPresenter,
    scoreboard: ScoreboardPresenter,
    stopwatch: StopwatchPresenter,
    roster: RosterPresenter,
) {
    coaching.reset()
    matchSetup.reset()
    history.clear()
    scoreboard.reset()
    stopwatch.reset()
    roster.reset()

    // Match setup
    matchSetup.gameId = report.game.gameId
    matchSetup.matchDate = report.game.matchDate
    matchSetup.homeTeamName = report.game.homeTeam
    matchSetup.guestTeamName = report.game.awayTeam
    matchSetup.firstRefereeName = report.firstReferee.firstName
    matchSetup.secondRefereeName = report.secondReferee.firstName

    // Criteria counts
    restoreCoachingFromReport(coaching, report)

    // History, scoreboard, stopwatch
    val restoredEntries = report.history.map { it.toCoachingHistoryEntry() }
    history.restore(restoredEntries)
    val last = restoredEntries.lastOrNull()
    if (last != null) {
        scoreboard.restore(last.homeScore, last.guestScore)
        stopwatch.setElapsed(last.gameTimeMillis.coerceAtLeast(0L))
    }

    // Rebuild rosters from attachments in the history.
    val homePlayers = mutableMapOf<String, Player>()
    val guestPlayers = mutableMapOf<String, Player>()
    restoredEntries.forEach { entry ->
        val attachment = entry.attachment ?: return@forEach
        val team = attachment.team ?: return@forEach
        val label = attachment.playerLabel?.trim() ?: return@forEach
        val id = attachment.playerId ?: return@forEach
        val player = Player(id = id, number = "", name = label)
        when (team) {
            RosterTeam.HOME -> homePlayers[id] = player
            RosterTeam.GUEST -> guestPlayers[id] = player
        }
    }
    roster.restore(home = homePlayers.values.toList(), guest = guestPlayers.values.toList())
}

private fun CoachingHistoryEntryDto.toCoachingHistoryEntry(): CoachingHistoryEntry =
    CoachingHistoryEntry(
        id = id,
        gameTimeMillis = gameTimeMillis,
        homeScore = homeScore,
        guestScore = guestScore,
        type = parseHistoryEventType(type),
        criterionId = criterionId,
        defectGroupId = defectGroupId,
        rootCauseId = rootCauseId,
        goalTeam = goalTeam?.let { parseRosterTeam(it) },
        selected = selected,
        attachment = if (team == null && teamLabel == null && playerId == null && playerLabel == null && refereeName == null) {
            null
        } else {
            CoachingHistoryAttachment(
                team = team?.let { parseRosterTeam(it) },
                teamLabel = teamLabel,
                playerId = playerId,
                playerLabel = playerLabel,
                refereeName = refereeName,
            )
        },
        note = note,
    )

private fun parseHistoryEventType(value: String): HistoryEventType =
    HistoryEventType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: HistoryEventType.ROOT_CAUSE

private fun parseRosterTeam(value: String): RosterTeam =
    when (value.uppercase()) {
        "HOME" -> RosterTeam.HOME
        "AWAY", "GUEST" -> RosterTeam.GUEST
        else -> RosterTeam.HOME
    }
