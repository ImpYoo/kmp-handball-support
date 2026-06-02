package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter

@Composable
fun MatchesSection(
    presenter: VoteAppPresenter,
) {
    Section(title = "2) Select Match") {
        if (presenter.matches.isEmpty()) {
            Text("No matches loaded")
            return@Section
        }
        presenter.matches.forEachIndexed { index, match ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            MatchRow(
                match = match,
                selected = presenter.selectedMatch?.id == match.id,
                onSelect = { presenter.chooseMatch(match) },
            )
        }
    }
}

@Preview
@Composable
private fun MatchesSectionPreviewEmpty() {
    val presenter = previewPresenter {
        matches = emptyList()
        selectedMatch = null
    }
    MaterialTheme {
        MatchesSection(presenter = presenter)
    }
}

@Preview
@Composable
private fun MatchesSectionPreviewLoaded() {
    val presenter = previewPresenter {
        matches = previewMatches
        selectedMatch = previewMatches.first()
    }
    MaterialTheme {
        MatchesSection(presenter = presenter)
    }
}


