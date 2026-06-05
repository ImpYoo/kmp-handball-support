package de.exhumedo.kmp.handball_support.ui
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbToggleButton

import de.exhumedo.kmp.handball_support.ui.theme.AppTheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.client.MatchResponseDto
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter
import de.exhumedo.kmp.handball_support.vote.VoteEvaluatorType

@Composable
fun VoteSection(
    presenter: VoteAppPresenter,
    onAction: (suspend () -> Unit) -> Unit,
) {
    Section(title = "3) Submit Vote") {
        val selectedMatch = presenter.selectedMatch
        if (selectedMatch == null) {
            Text("Select a match to vote")
            return@Section
        }

        SelectedMatchDetails(match = selectedMatch)
        Spacer(Modifier.height(12.dp))

        val refereeLabel = listOfNotNull(
            selectedMatch.refereeA?.name,
            selectedMatch.refereeB?.name,
        ).joinToString(" & ").ifBlank { "Referee team" }
        val delegateLabel = selectedMatch.delegate?.name ?: "Delegate"

        Row {
            DhbToggleButton(
                selected = presenter.evaluatorType == VoteEvaluatorType.REFEREE_TEAM,
                onClick = { presenter.updateEvaluatorType(VoteEvaluatorType.REFEREE_TEAM) },
            ) { Text(refereeLabel) }
            if (selectedMatch.delegate != null) {
                Spacer(Modifier.width(8.dp))
                DhbToggleButton(
                    selected = presenter.evaluatorType == VoteEvaluatorType.DELEGATE,
                    onClick = { presenter.updateEvaluatorType(VoteEvaluatorType.DELEGATE) },
                ) { Text(delegateLabel) }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (presenter.hasExistingVote) {
            Text("Vote already submitted for this match and evaluator type", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        ScoreTileRow(
            label = "Appearance",
            value = presenter.appearance,
            onValueChange = { presenter.appearance = it },
            enabled = !presenter.hasExistingVote,
        )
        ScoreTileRow(
            label = "Influence",
            value = presenter.influence,
            onValueChange = { presenter.influence = it },
            enabled = !presenter.hasExistingVote,
        )
        ScoreTileRow(
            label = "Teamwork",
            value = presenter.teamwork,
            onValueChange = { presenter.teamwork = it },
            enabled = !presenter.hasExistingVote,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = presenter.comment,
            onValueChange = { presenter.comment = it },
            label = { Text("Comment") },
            enabled = !presenter.hasExistingVote,
        )

        Spacer(Modifier.height(8.dp))

        var showConfirmDialog by remember { mutableStateOf(false) }

        DhbButton(
            onClick = { showConfirmDialog = true },
            enabled = !presenter.isBusy && !presenter.hasExistingVote,
        ) {
            Text("Submit Vote")
        }

        if (showConfirmDialog) {
            val evaluatorName = if (presenter.evaluatorType == VoteEvaluatorType.REFEREE_TEAM) {
                refereeLabel
            } else {
                delegateLabel
            }
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Submit vote?") },
                text = {
                    Column {
                        Text("${selectedMatch.homeTeam.name} vs ${selectedMatch.awayTeam.name}")
                        Spacer(Modifier.height(8.dp))
                        Text("Evaluator: $evaluatorName")
                        Spacer(Modifier.height(8.dp))
                        ScoreSummaryRow(label = "Appearance", value = presenter.appearance)
                        ScoreSummaryRow(label = "Influence", value = presenter.influence)
                        ScoreSummaryRow(label = "Teamwork", value = presenter.teamwork)
                        if (presenter.comment.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Comment: ${presenter.comment}")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This action cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                confirmButton = {
                    DhbButton(
                        onClick = {
                            showConfirmDialog = false
                            onAction { presenter.submitVote() }
                        },
                    ) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    DhbButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

/** A label + colored smiley tile row used in the submit-confirmation summary. */
@Composable
private fun ScoreSummaryRow(label: String, value: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(
            text = "$label:",
            modifier = Modifier.width(110.dp),
        )
        ScoreValueTile(value = value, size = 40.dp)
    }
}

@Composable
private fun SelectedMatchDetails(match: MatchResponseDto) {
    Column {
        Text(
            text = "${match.homeTeam.name}  vs  ${match.awayTeam.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        val dateTime = formatMatchDateTime(match.timestamp)
        if (dateTime.isNotEmpty()) {
            Text(
                text = dateTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        if (match.result.isNotBlank()) {
            OfficialLine(label = "Final", value = match.result)
        }
        if (match.halftimeResult.isNotBlank()) {
            OfficialLine(label = "Half time", value = match.halftimeResult)
        }
        match.timekeeper?.let { OfficialLine(label = "Timekeeper", value = it.name) }
        match.scorekeeper?.let { OfficialLine(label = "Scorekeeper", value = it.name) }
    }
}

@Preview
@Composable
private fun VoteSectionPreviewNoMatch() {
    val presenter = previewPresenter {
        selectedMatch = null
    }
    AppTheme {
        VoteSection(
            presenter = presenter,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun VoteSectionPreviewRefereeTeam() {
    val presenter = previewPresenter {
        withLoadedData(selectedMatchIndex = 0)
        evaluatorType = VoteEvaluatorType.REFEREE_TEAM
        isBusy = false
    }
    AppTheme {
        VoteSection(
            presenter = presenter,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun VoteSectionPreviewDelegateBusy() {
    val presenter = previewPresenter {
        withLoadedData(selectedMatchIndex = 1)
        evaluatorType = VoteEvaluatorType.DELEGATE
        isBusy = true
        comment = "Delegate perspective"
    }
    AppTheme {
        VoteSection(
            presenter = presenter,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun VoteSectionPreviewExistingVote() {
    val presenter = previewPresenter {
        withLoadedData(selectedMatchIndex = 0)
        evaluatorType = VoteEvaluatorType.REFEREE_TEAM
        hasExistingVote = true
        appearance = "4"
        influence = "5"
        teamwork = "3"
    }
    AppTheme {
        VoteSection(
            presenter = presenter,
            onAction = {},
        )
    }
}


