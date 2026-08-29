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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.matchconsole.MatchSetupPresenter
import de.exhumedo.kmp.handball_support.matchconsole.RosterPresenter
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbDialog
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Coaching preparation: define the match data (teams + referees) and the team
 * rosters, then continue to the live coaching session.
 *
 * "Weiter zum Coaching" is enabled once both team names are filled in. When a
 * session already has data ([isSessionActive]), a reset-everything action is
 * offered (with confirmation).
 */
@Composable
fun CoachingSetupScreen(
    matchSetup: MatchSetupPresenter,
    roster: RosterPresenter,
    isSessionActive: Boolean,
    username: String,
    password: String,
    token: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onResetAll: () -> Unit,
    onContinue: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val canContinue = matchSetup.homeTeamName.isNotBlank() && matchSetup.guestTeamName.isNotBlank()
    var showResetDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Coaching vorbereiten",
                subtitle = "Mannschaften, Schiedsrichter und Aufstellungen",
                onLogoClick = onNavigateHome,
                actions = {
                    if (token == null) {
                        DhbButton(onClick = { showLoginDialog = true }) { Text("Anmelden") }
                    } else {
                        DhbButton(onClick = onLogout) { Text("Abmelden ($username)") }
                    }
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
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    if (isSessionActive) {
                        ActiveSessionCard(
                            onContinue = onContinue,
                            onResetAll = { showResetDialog = true },
                        )
                    }

                    MatchSetupForm(setup = matchSetup)

                    Text(
                        text = "Aufstellungen",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    RosterEditor(roster = roster)

                    Spacer(Modifier.height(Dimens.spaceSm))
                    DhbButton(onClick = onContinue, enabled = canContinue) {
                        Text("Weiter zum Coaching")
                    }
                    if (!canContinue) {
                        Text(
                            text = "Bitte zuerst Heim- und Gastmannschaft benennen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(Dimens.spaceXl))
                }
            }
        }
    }

    if (showLoginDialog) {
        CoachingLoginDialog(
            username = username,
            password = password,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onDismiss = { showLoginDialog = false },
            onLogin = {
                onLogin()
                showLoginDialog = false
            },
        )
    }

    if (showResetDialog) {
        DhbDialog(
            onDismissRequest = { showResetDialog = false },
            title = "Alles zurücksetzen?",
            confirmText = "Alles zurücksetzen",
            dismissText = "Abbrechen",
            onConfirm = {
                onResetAll()
                showResetDialog = false
            },
        ) {
            Text(
                text = "Spieluhr, Anzeigetafel, Aufstellungen, Spieldaten, Coaching-Bogen und " +
                    "Verlauf werden vollständig zurückgesetzt.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ActiveSessionCard(
    onContinue: () -> Unit,
    onResetAll: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.cardElevation,
    ) {
        Column(modifier = Modifier.padding(Dimens.spaceLg)) {
            Text(
                text = "Laufende Coaching-Sitzung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(Dimens.spaceXs))
            Text(
                text = "Es liegen bereits Sitzungsdaten vor. Du kannst fortfahren oder alles zurücksetzen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Dimens.spaceMd))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                DhbButton(onClick = onContinue) { Text("Coaching fortsetzen") }
                DhbButton(onClick = onResetAll) { Text("Alles zurücksetzen") }
            }
        }
    }
}

@Preview
@Composable
private fun CoachingSetupScreenPreview() {
    CoachingSetupScreen(
        matchSetup = remember {
            MatchSetupPresenter().apply {
                homeTeamName = "THW Kiel"; homeTeamAbbreviation = "THW"
                guestTeamName = "SC Magdeburg"; guestTeamAbbreviation = "SCM"
                firstRefereeName = "Lemme"; secondRefereeName = "Ullrich"
            }
        },
        roster = remember { RosterPresenter().apply { addPlayer(de.exhumedo.kmp.handball_support.matchconsole.RosterTeam.HOME, "7", "Müller") } },
        isSessionActive = true,
        username = "admin",
        password = "",
        token = null,
        onUsernameChange = {},
        onPasswordChange = {},
        onLogin = {},
        onLogout = {},
        onResetAll = {},
        onContinue = {},
        onNavigateHome = {},
    )
}

@Composable
private fun CoachingLoginDialog(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onLogin: () -> Unit,
) {
    DhbDialog(
        onDismissRequest = onDismiss,
        title = "Anmelden",
        confirmText = "Anmelden",
        dismissText = "Abbrechen",
        onConfirm = onLogin,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Benutzername") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Passwort") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}





