package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter

@Composable
fun PhasesSection(
    presenter: VoteAppPresenter,
    onAction: (suspend () -> Unit) -> Unit,
) {
    Section(title = "1) Select Phase") {
        Button(onClick = { onAction { presenter.loadPhases() } }, enabled = presenter.token != null && !presenter.isBusy) {
            Text("Load Phases")
        }
        Spacer(Modifier.height(8.dp))
        if (presenter.phases.isEmpty()) {
            Text("No phases loaded")
        } else {
            presenter.phases.forEach { phase ->
                val selected = presenter.selectedPhaseId == phase.phaseId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "${phase.shortName} (${phase.phaseId})",
                    )
                    OutlinedButton(onClick = { onAction { presenter.loadMatches(phase.phaseId) } }) {
                        Text(if (selected) "Selected" else "Use")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PhasesSectionPreviewEmpty() {
    val presenter = previewPresenter {
        token = "preview-token"
        phases = emptyList()
    }
    MaterialTheme {
        PhasesSection(
            presenter = presenter,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PhasesSectionPreviewLoaded() {
    val presenter = previewPresenter {
        token = "preview-token"
        phases = previewPhases
        selectedPhaseId = previewPhases.first().phaseId
    }
    MaterialTheme {
        PhasesSection(
            presenter = presenter,
            onAction = {},
        )
    }
}


