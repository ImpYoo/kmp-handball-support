package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.exhumedo.kmp.handball_support.coaching.RefereeCoachingPresenter
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Standalone coaching sheet screen — the HVNB "Beobachterbogen" on its own,
 * without the stopwatch/scoreboard of the full coaching session.
 *
 * Renders the reusable [coachingSheet] in its own scroll container.
 */
@Composable
fun CoachingSheetScreen(
    coaching: RefereeCoachingPresenter,
    onNavigateHome: () -> Unit,
) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Coaching-Bogen",
                subtitle = "HVNB Beobachterbogen",
                onLogoClick = onNavigateHome,
                actions = {
                    DhbButton(onClick = coaching::reset) { Text("Zurücksetzen") }
                    Spacer(Modifier.width(Dimens.spaceSm))
                    DhbButton(onClick = onNavigateHome) { Text("Menü") }
                },
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = Dimens.contentMaxWidth)
                        .fillMaxWidth()
                        .padding(Dimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    coachingSheet(presenter = coaching, expanded = expanded)
                    item(key = "sheet-footer") { Spacer(Modifier.height(Dimens.spaceXl)) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CoachingSheetScreenPreview() {
    CoachingSheetScreen(
        coaching = remember { RefereeCoachingPresenter() },
        onNavigateHome = {},
    )
}

