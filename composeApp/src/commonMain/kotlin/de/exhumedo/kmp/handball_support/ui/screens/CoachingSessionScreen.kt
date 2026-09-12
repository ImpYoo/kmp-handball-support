package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryEntry
import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryPresenter
import de.exhumedo.kmp.handball_support.coaching.CoachingSessionSync
import de.exhumedo.kmp.handball_support.coaching.RefereeCoachingPresenter
import de.exhumedo.kmp.handball_support.matchconsole.MatchSetupPresenter
import de.exhumedo.kmp.handball_support.matchconsole.RosterPresenter
import de.exhumedo.kmp.handball_support.matchconsole.RosterTeam
import de.exhumedo.kmp.handball_support.matchconsole.ScoreboardPresenter
import de.exhumedo.kmp.handball_support.matchconsole.StopwatchPresenter
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens
import de.exhumedo.kmp.handball_support.ui.UserMenuButton
import de.exhumedo.kmp.handball_support.ui.components.CoachingReportCard

/**
 * "Coaching durchführen" — the live coaching session: stopwatch, scoreboard,
 * selection history and the coaching sheet, glued to the match setup and rosters.
 *
 * Tapping a history entry opens a dialog to connect it to a team, a roster
 * player and/or a referee.
 */
@Composable
fun CoachingSessionScreen(
    coaching: RefereeCoachingPresenter,
    sync: CoachingSessionSync,
    stopwatch: StopwatchPresenter,
    scoreboard: ScoreboardPresenter,
    history: CoachingHistoryPresenter,
    roster: RosterPresenter,
    matchSetup: MatchSetupPresenter,
    username: String,
    isLoggedIn: Boolean,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var selectedEntryId by remember { mutableStateOf<String?>(null) }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Schiedsrichter-Coaching",
                subtitle = sessionSubtitle(matchSetup),
                onLogoClick = onNavigateHome,
                actions = {
                    SyncStatusChip(sync.status)
                    Spacer(Modifier.width(Dimens.spaceSm))
                    UserMenuButton(
                        isLoggedIn = isLoggedIn,
                        username = username,
                        onSettings = onOpenSettings,
                        onLogin = { },
                        onLogout = onLogout,
                    )
                },
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = Dimens.contentMaxWidth)
                        .fillMaxWidth()
                        .padding(Dimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    item(key = "sync-report") {
                        sync.report?.let { report ->
                            CoachingReportCard(report = report)
                        }
                    }
                    item(key = "session-stopwatch") { StopwatchPanel(stopwatch = stopwatch) }
                    item(key = "session-toolbar") {
                        SessionToolbar(
                            onResetSheet = {
                                coaching.reset()
                                history.clear()
                            },
                        )
                    }
                    item(key = "session-scoreboard") {
                        ScoreboardPanel(
                            scoreboard = scoreboard,
                            onHomeChange = { added ->
                                history.recordGoal(
                                    gameTimeMillis = stopwatch.elapsedMillis,
                                    homeScore = scoreboard.homeScore,
                                    guestScore = scoreboard.guestScore,
                                    team = RosterTeam.HOME,
                                    scored = added,
                                )
                            },
                            onGuestChange = { added ->
                                history.recordGoal(
                                    gameTimeMillis = stopwatch.elapsedMillis,
                                    homeScore = scoreboard.homeScore,
                                    guestScore = scoreboard.guestScore,
                                    team = RosterTeam.GUEST,
                                    scored = added,
                                )
                            },
                        )
                    }
                    coachingSheet(
                        presenter = coaching,
                        expanded = expanded,
                        onSelected = { criterionId, groupId, rootCauseId ->
                            history.record(
                                gameTimeMillis = stopwatch.elapsedMillis,
                                homeScore = scoreboard.homeScore,
                                guestScore = scoreboard.guestScore,
                                criterionId = criterionId,
                                defectGroupId = groupId,
                                rootCauseId = rootCauseId,
                                selected = true,
                            )
                        },
                        onDeselected = { criterionId, groupId, rootCauseId ->
                            history.record(
                                gameTimeMillis = stopwatch.elapsedMillis,
                                homeScore = scoreboard.homeScore,
                                guestScore = scoreboard.guestScore,
                                criterionId = criterionId,
                                defectGroupId = groupId,
                                rootCauseId = rootCauseId,
                                selected = false,
                            )
                        },
                    )

                    item(key = "session-history") {
                        CoachingHistoryPanel(
                            entries = history.entriesDescending,
                            onEntryClick = { id -> selectedEntryId = id },
                        )
                    }

                    item(key = "session-footer") { Spacer(Modifier.height(Dimens.spaceXl)) }
                }
            }
        }
    }

    val entryId = selectedEntryId
    val entry: CoachingHistoryEntry? = entryId?.let { history.find(it) }
    if (entry != null) {
        HistoryAttachmentDialog(
            homeTeamLabel = teamLabel(matchSetup.homeTeamAbbreviation, matchSetup.homeTeamName, "Heim"),
            guestTeamLabel = teamLabel(matchSetup.guestTeamAbbreviation, matchSetup.guestTeamName, "Gast"),
            homePlayers = roster.homePlayers,
            guestPlayers = roster.guestPlayers,
            referees = listOfNotNull(
                matchSetup.firstRefereeName.takeIf { it.isNotBlank() },
                matchSetup.secondRefereeName.takeIf { it.isNotBlank() },
            ),
            initial = entry.attachment,
            initialNote = entry.note,
            onDismiss = { selectedEntryId = null },
            onConfirm = { attachment, note ->
                history.attach(entry.id, attachment)
                history.setNote(entry.id, note)
                selectedEntryId = null
            },
        )
    }
}

@Composable
private fun SessionToolbar(onResetSheet: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        DhbButton(onClick = onResetSheet) { Text("Bogen zurücksetzen") }
    }
}

private fun sessionSubtitle(setup: MatchSetupPresenter): String {
    val home = setup.homeTeamAbbreviation.ifBlank { setup.homeTeamName }
    val guest = setup.guestTeamAbbreviation.ifBlank { setup.guestTeamName }
    return if (home.isNotBlank() && guest.isNotBlank()) "$home vs $guest" else "Coaching-Sitzung"
}

private fun teamLabel(abbreviation: String, name: String, fallback: String): String =
    abbreviation.ifBlank { name }.ifBlank { fallback }

@Composable
private fun SyncStatusChip(status: CoachingSessionSync.SyncStatus) {
    val (label, color) = when (status) {
        is CoachingSessionSync.SyncStatus.Idle -> "Bereit" to MaterialTheme.colorScheme.outline
        is CoachingSessionSync.SyncStatus.Syncing -> "Speichern..." to MaterialTheme.colorScheme.primary
        is CoachingSessionSync.SyncStatus.Success -> "Gespeichert" to MaterialTheme.colorScheme.tertiary
        is CoachingSessionSync.SyncStatus.Offline -> "Offline" to MaterialTheme.colorScheme.outlineVariant
        is CoachingSessionSync.SyncStatus.Error -> "Fehler" to MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Preview
@Composable
private fun CoachingSessionScreenPreview() {
    CoachingSessionScreen(
        coaching = remember { RefereeCoachingPresenter() },
        sync = remember { CoachingSessionSync() },
        stopwatch = remember { StopwatchPresenter().apply { setElapsed(12 * 60_000L + 5_000L) } },
        scoreboard = remember { ScoreboardPresenter().apply { repeat(14) { incrementHome() }; repeat(12) { incrementGuest() } } },
        history = remember { CoachingHistoryPresenter() },
        roster = remember { RosterPresenter() },
        matchSetup = remember { MatchSetupPresenter() },
        username = "coach",
        isLoggedIn = true,
        onOpenSettings = {},
        onLogout = {},
        onNavigateHome = {},
    )
}

