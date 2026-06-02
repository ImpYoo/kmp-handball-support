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
import de.exhumedo.kmp.handball_support.vote.VoteEvaluatorType

@Composable
fun VoteFormScreen(
    presenter: VoteAppPresenter,
    onAction: (suspend () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vote Submission",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
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
                VoteSection(presenter = presenter, onAction = onAction)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Preview
@Composable
private fun VoteFormScreenPreview() {
    val presenter = previewPresenter {
        withLoadedData(selectedMatchIndex = 0)
        evaluatorType = VoteEvaluatorType.REFEREE_TEAM
        isBusy = false
    }
    VoteFormScreen(
        presenter = presenter,
        onAction = {},
        onBack = {},
    )
}

@Preview
@Composable
private fun VoteFormScreenPreviewExistingVote() {
    val presenter = previewPresenter {
        withLoadedData(selectedMatchIndex = 1)
        evaluatorType = VoteEvaluatorType.DELEGATE
        hasExistingVote = true
        appearance = "4"
        influence = "5"
        teamwork = "3"
    }
    VoteFormScreen(
        presenter = presenter,
        onAction = {},
        onBack = {},
    )
}

