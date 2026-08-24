package de.exhumedo.kmp.handball_support.ui
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter
import de.exhumedo.kmp.handball_support.vote.VoteEvaluatorType

@Composable
fun VoteFormScreen(
    presenter: VoteAppPresenter,
    onAction: (suspend () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Vote Submission",
                onLogoClick = onBack,
                actions = {
                    DhbButton(onClick = onBack) {
                        Text("Back")
                    }
                },
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = Dimens.contentMaxWidth)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(Dimens.spaceLg),
                ) {
                    VoteSection(presenter = presenter, onAction = onAction)
                    Spacer(Modifier.height(Dimens.spaceXl))
                }
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

