package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter

@Composable
fun PhasesScreen(
    presenter: VoteAppPresenter,
    onAction: (suspend () -> Unit) -> Unit,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(presenter.filterDay, presenter.filterMonth, presenter.filterYear) {
        onAction { presenter.loadPhases() }
    }

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
                        text = "Handball Support - Voting",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (presenter.token != null) "Logged in as ${presenter.role}" else "Not logged in",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (presenter.token != null) {
                    OutlinedButton(onClick = { presenter.logout() }) {
                        Text("Logout")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                Section(title = "Filter by date (optional)") {
                    // Stage typed values locally so the filter is committed only on Apply.
                    var dayInput by remember(presenter.filterDay) {
                        mutableStateOf(presenter.filterDay?.toString() ?: "")
                    }
                    var monthInput by remember(presenter.filterMonth) {
                        mutableStateOf(presenter.filterMonth?.toString() ?: "")
                    }
                    var yearInput by remember(presenter.filterYear) {
                        mutableStateOf(presenter.filterYear?.toString() ?: "")
                    }

                    val parsedDay = dayInput.takeIf { it.isNotBlank() }?.toIntOrNull()
                    val parsedMonth = monthInput.takeIf { it.isNotBlank() }?.toIntOrNull()
                    val parsedYear = yearInput.takeIf { it.isNotBlank() }?.toIntOrNull()
                    val isDirty = parsedDay != presenter.filterDay ||
                        parsedMonth != presenter.filterMonth ||
                        parsedYear != presenter.filterYear
                    val hasAnyFilter = parsedDay != null || parsedMonth != null || parsedYear != null ||
                        presenter.filterDay != null || presenter.filterMonth != null || presenter.filterYear != null

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = dayInput,
                            onValueChange = { value -> dayInput = value.filter { it.isDigit() } },
                            label = { Text("Day") },
                            singleLine = true,
                            modifier = Modifier.width(80.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = monthInput,
                            onValueChange = { value -> monthInput = value.filter { it.isDigit() } },
                            label = { Text("Month") },
                            singleLine = true,
                            modifier = Modifier.width(90.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { value -> yearInput = value.filter { it.isDigit() } },
                            label = { Text("Year") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                presenter.setDateFilter(
                                    day = parsedDay,
                                    month = parsedMonth,
                                    year = parsedYear,
                                )
                            },
                            enabled = isDirty,
                        ) {
                            Text("Apply filter")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                dayInput = ""
                                monthInput = ""
                                yearInput = ""
                                presenter.setDateFilter(day = null, month = null, year = null)
                            },
                            enabled = hasAnyFilter,
                        ) {
                            Text("Clear")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Section(title = "1) Select Phase") {
                    if (presenter.phases.isEmpty()) {
                        Text("No phases available")
                    } else {
                        presenter.phases.forEachIndexed { index, phase ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            PhaseCard(
                                phase = phase,
                                onOpen = { onAction { presenter.openPhase(phase.phaseId) } },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Text(
                    text = presenter.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Preview
@Composable
private fun PhasesScreenPreview() {
    val presenter = previewPresenter {
        token = "preview-token"
        role = "ADMIN"
        phases = previewPhases
    }
    PhasesScreen(
        presenter = presenter,
        onAction = {},
    )
}

@Preview
@Composable
private fun PhasesScreenPreviewWithMatches() {
    val presenter = previewPresenter {
        token = "preview-token"
        role = "ADMIN"
        phases = previewPhases
        selectedPhaseId = previewPhases.first().phaseId
    }
    PhasesScreen(
        presenter = presenter,
        onAction = {},
    )
}

