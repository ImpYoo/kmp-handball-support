package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryAttachment
import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryEntry
import de.exhumedo.kmp.handball_support.coaching.HistoryEventType
import de.exhumedo.kmp.handball_support.matchconsole.Player
import de.exhumedo.kmp.handball_support.matchconsole.RosterTeam
import de.exhumedo.kmp.handball_support.ui.theme.DhbToggleButton
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Shows the history of selected root causes, newest first, in the form:
 *
 * `HH:MM  X:X  {criterionId}, {defectGroupId}: {rootCauseId}`
 *
 * with any attached team/player/referee appended. Tapping an entry triggers
 * [onEntryClick] so the caller can open the attachment editor.
 */
@Composable
fun CoachingHistoryPanel(
    entries: List<CoachingHistoryEntry>,
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.cardElevation,
    ) {
        Column(modifier = Modifier.padding(Dimens.spaceLg)) {
            Text(
                text = "Verlauf",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Dimens.spaceMd))

            if (entries.isEmpty()) {
                Text(
                    text = "Noch keine Auswahl.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                entries.forEach { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEntryClick(entry.id) }
                            .padding(vertical = Dimens.spaceXs),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (entry.type == HistoryEventType.GOAL) {
                                    Icons.Filled.SportsSoccer
                                } else {
                                    Icons.Filled.Flag
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(Dimens.spaceSm))
                            Text(
                                text = formatHistoryEntry(entry),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        val tag = attachmentLabel(entry.attachment)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (tag != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(Dimens.spaceSm))
                            Text(
                                text = tag ?: "Zuordnung hinzufügen...",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (tag != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (entry.note.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(Dimens.spaceSm))
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatHistoryEntry(entry: CoachingHistoryEntry): String {
    val time = formatElapsed(entry.gameTimeMillis)
    val score = "${entry.homeScore}:${entry.guestScore}"
    val body = when (entry.type) {
        HistoryEventType.GOAL -> {
            val team = if (entry.goalTeam == RosterTeam.HOME) "Heim" else "Gast"
            if (entry.selected) "Tor $team" else "Tor-Korrektur $team"
        }
        HistoryEventType.ROOT_CAUSE -> {
            val sign = if (entry.selected) "+" else "-"
            "$sign ${entry.criterionId}, ${entry.defectGroupId}: ${entry.rootCauseId}"
        }
    }
    return "$time  $score  $body"
}

private fun attachmentLabel(attachment: CoachingHistoryAttachment?): String? {
    if (attachment == null || attachment.isEmpty) return null
    val parts = listOfNotNull(
        attachment.teamLabel,
        attachment.playerLabel,
        attachment.refereeName?.let { "SR $it" },
    )
    return parts.joinToString(" · ").ifEmpty { null }
}

/**
 * Lets the user connect a history entry to a team, a roster player and/or a
 * referee. Any combination (including all three) is allowed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryAttachmentDialog(
    homeTeamLabel: String,
    guestTeamLabel: String,
    homePlayers: List<Player>,
    guestPlayers: List<Player>,
    referees: List<String>,
    initial: CoachingHistoryAttachment?,
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (CoachingHistoryAttachment, String) -> Unit,
) {
    var selectedTeam by remember { mutableStateOf(initial?.team) }
    var selectedPlayerId by remember { mutableStateOf(initial?.playerId) }
    var selectedReferee by remember { mutableStateOf(initial?.refereeName) }
    var note by remember { mutableStateOf(initialNote) }

    val players = when (selectedTeam) {
        RosterTeam.HOME -> homePlayers
        RosterTeam.GUEST -> guestPlayers
        null -> emptyList()
    }

    de.exhumedo.kmp.handball_support.ui.theme.DhbDialog(
        onDismissRequest = onDismiss,
        title = "Zuordnung",
        confirmText = "Übernehmen",
        dismissText = "Abbrechen",
        onConfirm = {
            val player = players.firstOrNull { it.id == selectedPlayerId }
            onConfirm(
                CoachingHistoryAttachment(
                    team = selectedTeam,
                    teamLabel = when (selectedTeam) {
                        RosterTeam.HOME -> homeTeamLabel
                        RosterTeam.GUEST -> guestTeamLabel
                        null -> null
                    },
                    playerId = player?.id,
                    playerLabel = player?.let { playerLabel(it) },
                    refereeName = selectedReferee,
                ),
                note,
            )
        },
    ) {
        Text("Mannschaft", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.spaceSm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            DhbToggleButton(selected = selectedTeam == null, onClick = { selectedTeam = null; selectedPlayerId = null }) { Text("Keine") }
            DhbToggleButton(
                selected = selectedTeam == RosterTeam.HOME,
                onClick = { selectedTeam = RosterTeam.HOME; selectedPlayerId = null },
            ) { Text(homeTeamLabel) }
            DhbToggleButton(
                selected = selectedTeam == RosterTeam.GUEST,
                onClick = { selectedTeam = RosterTeam.GUEST; selectedPlayerId = null },
            ) { Text(guestTeamLabel) }
        }

        if (selectedTeam != null) {
            Spacer(Modifier.height(Dimens.spaceMd))
            Text("Spieler", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Dimens.spaceSm))
            if (players.isEmpty()) {
                Text(
                    text = "Keine Spieler in dieser Aufstellung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                        DhbToggleButton(selected = selectedPlayerId == null, onClick = { selectedPlayerId = null }) { Text("Keiner") }
                        players.forEach { player ->
                            DhbToggleButton(
                                selected = selectedPlayerId == player.id,
                                onClick = { selectedPlayerId = player.id },
                            ) { Text(playerLabel(player)) }
                        }
                    }
                }
            }
        }

        if (referees.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.spaceMd))
            Text("Schiedsrichter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Dimens.spaceSm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                DhbToggleButton(selected = selectedReferee == null, onClick = { selectedReferee = null }) { Text("Keiner") }
                referees.forEach { name ->
                    DhbToggleButton(
                        selected = selectedReferee == name,
                        onClick = { selectedReferee = name },
                    ) { Text(name) }
                }
            }
        }

        Spacer(Modifier.height(Dimens.spaceMd))
        Text("Notiz", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.spaceSm))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Notiz zu diesem Ereignis") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}

private fun playerLabel(player: Player): String =
    if (player.number.isBlank()) player.name else "#${player.number} ${player.name}"

