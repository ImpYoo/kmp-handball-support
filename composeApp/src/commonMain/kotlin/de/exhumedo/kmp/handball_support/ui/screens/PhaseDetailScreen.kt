package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter

@Composable
fun PhaseDetailScreen(
    presenter: VoteAppPresenter,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phase ${presenter.selectedPhaseId ?: "-"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (presenter.token != null) "Logged in as ${presenter.role}" else "Not logged in",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                MatchesSection(presenter = presenter)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = presenter.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PhaseDetailScreenPreview() {
    val presenter = previewPresenter {
        selectedPhaseId = previewPhases.first().phaseId
        matches = previewMatches
        statusMessage = "Loaded 2 matches"
    }
    PhaseDetailScreen(
        presenter = presenter,
        onBack = {},
    )
}

