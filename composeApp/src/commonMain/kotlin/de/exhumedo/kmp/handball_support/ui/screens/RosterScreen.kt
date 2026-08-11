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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.matchconsole.Player
import de.exhumedo.kmp.handball_support.matchconsole.RosterPresenter
import de.exhumedo.kmp.handball_support.matchconsole.RosterTeam
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbDialog
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Reusable roster editor: two side-by-side team panels (Heim/Gast) with add and
 * remove, including the add-player dialog. Used by [RosterScreen] and the
 * coaching setup flow.
 */
@Composable
fun RosterEditor(
    roster: RosterPresenter,
    modifier: Modifier = Modifier,
) {
    var addDialogTeam by remember { mutableStateOf<RosterTeam?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        TeamRosterPanel(
            title = "Heim",
            players = roster.homePlayers,
            onAdd = { addDialogTeam = RosterTeam.HOME },
            onRemove = { id -> roster.removePlayer(RosterTeam.HOME, id) },
            modifier = Modifier.weight(1f),
        )
        TeamRosterPanel(
            title = "Gast",
            players = roster.guestPlayers,
            onAdd = { addDialogTeam = RosterTeam.GUEST },
            onRemove = { id -> roster.removePlayer(RosterTeam.GUEST, id) },
            modifier = Modifier.weight(1f),
        )
    }

    val team = addDialogTeam
    if (team != null) {
        AddPlayerDialog(
            teamLabel = if (team == RosterTeam.HOME) "Heim" else "Gast",
            onDismiss = { addDialogTeam = null },
            onConfirm = { number, name ->
                val added = roster.addPlayer(team, number, name)
                if (added) addDialogTeam = null
            },
        )
    }
}

/**
 * "Aufstellungen" — standalone screen to manage the home and guest team rosters.
 */
@Composable
fun RosterScreen(
    roster: RosterPresenter,
    onNavigateHome: () -> Unit,
) {
    val scrollState = rememberScrollState()

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Aufstellungen",
                subtitle = "Heim- und Gastmannschaft",
                actions = {
                    DhbButton(onClick = roster::reset) { Text("Zurücksetzen") }
                    Spacer(Modifier.width(Dimens.spaceSm))
                    DhbButton(onClick = onNavigateHome) { Text("Menü") }
                },
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = Dimens.contentMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(Dimens.spaceLg),
                ) {
                    RosterEditor(roster = roster)
                }
            }
        }
    }
}

@Composable
private fun TeamRosterPanel(
    title: String,
    players: List<Player>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${players.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Dimens.spaceMd))

            if (players.isEmpty()) {
                Text(
                    text = "Noch keine Spieler.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                players.forEach { player ->
                    PlayerRow(player = player, onRemove = { onRemove(player.id) })
                    Spacer(Modifier.height(Dimens.spaceXs))
                }
            }

            Spacer(Modifier.height(Dimens.spaceMd))
            DhbButton(onClick = onAdd) { Text("+ Spieler") }
        }
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(36.dp)) {
            Text(
                text = player.number,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(Dimens.spaceSm))
        Text(
            text = player.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        DhbButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Entfernen")
        }
    }
}

@Composable
private fun AddPlayerDialog(
    teamLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (number: String, name: String) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    DhbDialog(
        onDismissRequest = onDismiss,
        title = "Spieler hinzufügen ($teamLabel)",
        confirmText = "Hinzufügen",
        dismissText = "Abbrechen",
        onConfirm = { onConfirm(number, name) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = number,
                onValueChange = { value -> number = value.filter { it.isDigit() }.take(3) },
                label = { Text("Nr.") },
                singleLine = true,
                modifier = Modifier.width(96.dp),
            )
            Spacer(Modifier.width(Dimens.spaceMd))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

