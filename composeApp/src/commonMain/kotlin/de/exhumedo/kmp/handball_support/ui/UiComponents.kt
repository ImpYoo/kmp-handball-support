package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.client.MatchResponseDto

@Composable
fun MatchRow(
    match: MatchResponseDto,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "#${match.id} · ${formatMatchDateTime(match.timestamp)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onSelect) {
                Text(if (selected) "Selected" else "Select")
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${match.homeTeam.name}  vs  ${match.awayTeam.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (match.result.isNotBlank() || match.halftimeResult.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = matchScoreLine(match),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
        MatchOfficialsBlock(match = match)
    }
}

@Composable
private fun MatchOfficialsBlock(match: MatchResponseDto) {
    Column {
        OfficialLine(label = "Referees", value = formatRefereePair(match))
        match.delegate?.let { OfficialLine(label = "Delegate", value = it.name) }
    }
}

private fun formatRefereePair(match: MatchResponseDto): String {
    val a = match.refereeA?.name
    val b = match.refereeB?.name
    return when {
        a != null && b != null -> "$a, $b"
        a != null -> a
        b != null -> b
        else -> "TBD"
    }
}

@Composable
internal fun OfficialLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

internal fun matchScoreLine(match: MatchResponseDto): String = buildString {
    if (match.result.isNotBlank()) append(match.result)
    if (match.halftimeResult.isNotBlank()) {
        if (isNotEmpty()) append("  ")
        append("(HT ").append(match.halftimeResult).append(")")
    }
}

@Composable
fun ScoreField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        enabled = enabled,
        readOnly = !enabled,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Preview
@Composable
private fun MatchRowPreviewUnselected() {
    MaterialTheme {
        MatchRow(
            match = previewMatches.first(),
            selected = false,
            onSelect = {},
        )
    }
}

@Preview
@Composable
private fun MatchRowPreviewSelected() {
    MaterialTheme {
        MatchRow(
            match = previewMatches.first(),
            selected = true,
            onSelect = {},
        )
    }
}

@Preview
@Composable
private fun ScoreFieldPreview() {
    MaterialTheme {
        ScoreField(
            label = "Appearance (1-5)",
            value = "4",
            onValueChange = {},
        )
    }
}

@Preview
@Composable
private fun ScoreFieldPreviewDisabled() {
    MaterialTheme {
        ScoreField(
            label = "Appearance (Read-only)",
            value = "4",
            onValueChange = {},
            enabled = false,
        )
    }
}

@Preview
@Composable
private fun SectionPreview() {
    MaterialTheme {
        Section(title = "Preview Section") {
            Text("Section body content")
        }
    }
}


