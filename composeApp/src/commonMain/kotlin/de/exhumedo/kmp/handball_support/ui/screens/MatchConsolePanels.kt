package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.matchconsole.ScoreboardPresenter
import de.exhumedo.kmp.handball_support.matchconsole.StopwatchPresenter
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbDialog
import de.exhumedo.kmp.handball_support.ui.theme.Dimens
import kotlinx.coroutines.delay

/**
 * Self-contained stopwatch panel: mm:ss display, start/stop, edit-time dialog and
 * reset-with-confirmation dialog. Drives its own refresh loop while running.
 *
 * Reusable across the standalone match console and the coaching session screen.
 */
@Composable
fun StopwatchPanel(
    stopwatch: StopwatchPresenter,
    modifier: Modifier = Modifier,
) {
    // Refresh the displayed time ~5x/second while running. The monotonic clock in
    // the presenter keeps the value accurate regardless of tick cadence.
    LaunchedEffect(stopwatch.isRunning) {
        while (stopwatch.isRunning) {
            stopwatch.tick()
            delay(200)
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    ConsoleCard(title = "Spieluhr", modifier = modifier) {
        Text(
            text = formatElapsed(stopwatch.elapsedMillis),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(Dimens.spaceLg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm, Alignment.CenterHorizontally),
        ) {
            DhbButton(onClick = stopwatch::toggle) { Text(if (stopwatch.isRunning) "Stopp" else "Start") }
            DhbButton(onClick = { showEditDialog = true }) { Text("Bearbeiten") }
            DhbButton(onClick = { showResetDialog = true }) { Text("Zurücksetzen") }
        }
    }

    if (showEditDialog) {
        EditTimeDialog(
            initialMillis = stopwatch.elapsedMillis,
            onDismiss = { showEditDialog = false },
            onConfirm = { millis ->
                stopwatch.setElapsed(millis)
                showEditDialog = false
            },
        )
    }

    if (showResetDialog) {
        DhbDialog(
            onDismissRequest = { showResetDialog = false },
            title = "Spieluhr zurücksetzen?",
            confirmText = "Zurücksetzen",
            dismissText = "Abbrechen",
            onConfirm = {
                stopwatch.reset()
                showResetDialog = false
            },
        ) {
            Text(
                text = "Die gestoppte Zeit wird auf 00:00 zurückgesetzt.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Two-team scoreboard panel with +/- controls. Scores never drop below zero.
 *
 * [onHomeChange]/[onGuestChange] are invoked after a change with `added` = true
 * for an increment (+) and false for a decrement (−), e.g. to log goals.
 */
@Composable
fun ScoreboardPanel(
    scoreboard: ScoreboardPresenter,
    modifier: Modifier = Modifier,
    onHomeChange: (added: Boolean) -> Unit = {},
    onGuestChange: (added: Boolean) -> Unit = {},
) {
    ConsoleCard(title = "Anzeigetafel", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamScoreColumn(
                label = "Heim",
                score = scoreboard.homeScore,
                onIncrement = { scoreboard.incrementHome(); onHomeChange(true) },
                onDecrement = { scoreboard.decrementHome(); onHomeChange(false) },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Dimens.spaceMd),
            )
            TeamScoreColumn(
                label = "Gast",
                score = scoreboard.guestScore,
                onIncrement = { scoreboard.incrementGuest(); onGuestChange(true) },
                onDecrement = { scoreboard.decrementGuest(); onGuestChange(false) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TeamScoreColumn(
    label: String,
    score: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.spaceSm))
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(Dimens.spaceSm))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            DhbButton(onClick = onDecrement, enabled = score > 0) { Text("-") }
            DhbButton(onClick = onIncrement) { Text("+") }
        }
    }
}

@Composable
private fun ConsoleCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.cardElevation,
    ) {
        Column(modifier = Modifier.padding(Dimens.spaceLg)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Dimens.spaceLg))
            content()
        }
    }
}

@Composable
private fun EditTimeDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val totalSeconds = initialMillis / 1000
    var minutes by remember { mutableStateOf((totalSeconds / 60).toString()) }
    var seconds by remember { mutableStateOf((totalSeconds % 60).toString().padStart(2, '0')) }

    val parsedMinutes = minutes.toIntOrNull() ?: 0
    val parsedSeconds = (seconds.toIntOrNull() ?: 0).coerceIn(0, 59)
    val resultMillis = (parsedMinutes * 60L + parsedSeconds) * 1000L

    DhbDialog(
        onDismissRequest = onDismiss,
        title = "Zeit bearbeiten",
        confirmText = "Übernehmen",
        dismissText = "Abbrechen",
        onConfirm = { onConfirm(resultMillis) },
    ) {
        Text(
            text = "Verstrichene Zeit (mm:ss)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.spaceSm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = minutes,
                onValueChange = { value -> minutes = value.filter { it.isDigit() }.take(3) },
                label = { Text("Min") },
                singleLine = true,
                modifier = Modifier.width(96.dp),
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Dimens.spaceSm),
            )
            OutlinedTextField(
                value = seconds,
                onValueChange = { value -> seconds = value.filter { it.isDigit() }.take(2) },
                label = { Text("Sek") },
                singleLine = true,
                modifier = Modifier.width(96.dp),
            )
        }
    }
}

/** Formats milliseconds as mm:ss (minutes are not capped at 59). */
internal fun formatElapsed(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
}

