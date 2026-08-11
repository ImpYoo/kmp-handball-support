package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.exhumedo.kmp.handball_support.matchconsole.ScoreboardPresenter
import de.exhumedo.kmp.handball_support.matchconsole.StopwatchPresenter
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Standalone "Spieluhr & Anzeigetafel" tool: stopwatch and scoreboard only.
 * The panels themselves are reused by the coaching session screen.
 */
@Composable
fun MatchConsoleScreen(
    stopwatch: StopwatchPresenter,
    scoreboard: ScoreboardPresenter,
    onNavigateHome: () -> Unit,
) {
    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Spieluhr & Anzeigetafel",
                subtitle = "Zeitnahme und Spielstand",
                actions = {
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
                        .padding(Dimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    StopwatchPanel(stopwatch = stopwatch)
                    ScoreboardPanel(scoreboard = scoreboard)
                }
            }
        }
    }
}

@Preview
@Composable
private fun MatchConsoleScreenPreview() {
    MatchConsoleScreen(
        stopwatch = remember { StopwatchPresenter().apply { setElapsed(15 * 60_000L + 42_000L) } },
        scoreboard = remember { ScoreboardPresenter().apply { repeat(23) { incrementHome() }; repeat(21) { incrementGuest() } } },
        onNavigateHome = {},
    )
}

