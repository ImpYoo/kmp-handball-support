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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.matchconsole.MatchSetupPresenter
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Reusable form to define the match data: home/away teams (name + abbreviation)
 * and the two referees. Two-way bound to [MatchSetupPresenter].
 */
@Composable
fun MatchSetupForm(
    setup: MatchSetupPresenter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        FormCard(title = "Mannschaften") {
            TeamRow(
                teamLabel = "Heim",
                name = setup.homeTeamName,
                abbreviation = setup.homeTeamAbbreviation,
                onNameChange = { setup.homeTeamName = it },
                onAbbreviationChange = { setup.homeTeamAbbreviation = sanitizeAbbreviation(it) },
            )
            Spacer(Modifier.height(Dimens.spaceMd))
            TeamRow(
                teamLabel = "Gast",
                name = setup.guestTeamName,
                abbreviation = setup.guestTeamAbbreviation,
                onNameChange = { setup.guestTeamName = it },
                onAbbreviationChange = { setup.guestTeamAbbreviation = sanitizeAbbreviation(it) },
            )
        }

        FormCard(title = "Schiedsrichter") {
            OutlinedTextField(
                value = setup.firstRefereeName,
                onValueChange = { setup.firstRefereeName = it },
                label = { Text("Schiedsrichter 1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.spaceSm))
            OutlinedTextField(
                value = setup.secondRefereeName,
                onValueChange = { setup.secondRefereeName = it },
                label = { Text("Schiedsrichter 2") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Standalone screen wrapping [MatchSetupForm] with the app header.
 */
@Composable
fun MatchSetupScreen(
    setup: MatchSetupPresenter,
    onNavigateHome: () -> Unit,
) {
    val scrollState = rememberScrollState()

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Spieldaten",
                subtitle = "Mannschaften und Schiedsrichter",
                onLogoClick = onNavigateHome,
                actions = {
                    DhbButton(onClick = setup::reset) { Text("Zurücksetzen") }
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
                    MatchSetupForm(setup = setup)
                }
            }
        }
    }
}

@Composable
private fun TeamRow(
    teamLabel: String,
    name: String,
    abbreviation: String,
    onNameChange: (String) -> Unit,
    onAbbreviationChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = teamLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.spaceXs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Dimens.spaceMd))
            OutlinedTextField(
                value = abbreviation,
                onValueChange = onAbbreviationChange,
                label = { Text("Kürzel") },
                singleLine = true,
                modifier = Modifier.width(110.dp),
            )
        }
    }
}

@Composable
private fun FormCard(
    title: String,
    content: @Composable () -> Unit,
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Dimens.spaceLg))
            content()
        }
    }
}

/** Uppercases and trims an abbreviation to the configured maximum length. */
private fun sanitizeAbbreviation(raw: String): String =
    raw.filter { !it.isWhitespace() }
        .uppercase()
        .take(MatchSetupPresenter.MAX_ABBREVIATION_LENGTH)

@Preview
@Composable
private fun MatchSetupScreenPreview() {
    val setup = remember {
        MatchSetupPresenter().apply {
            homeTeamName = "THW Kiel"
            homeTeamAbbreviation = "THW"
            guestTeamName = "SC Magdeburg"
            guestTeamAbbreviation = "SCM"
            firstRefereeName = "Lemme"
            secondRefereeName = "Ullrich"
        }
    }
    MatchSetupScreen(setup = setup, onNavigateHome = {})
}

