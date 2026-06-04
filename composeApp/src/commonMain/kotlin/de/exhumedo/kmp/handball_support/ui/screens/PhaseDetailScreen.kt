package de.exhumedo.kmp.handball_support.ui
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbAccentRule
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter

@Composable
fun PhaseDetailScreen(
    presenter: VoteAppPresenter,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Phase ${presenter.selectedPhaseId ?: "-"}",
                subtitle = if (presenter.token != null) "Logged in as ${presenter.role}" else "Not logged in",
                actions = {
                    DhbButton(onClick = onBack) {
                        Text("Back")
                    }
                },
            )
            DhbAccentRule()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
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

