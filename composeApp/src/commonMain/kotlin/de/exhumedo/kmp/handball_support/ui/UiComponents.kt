package de.exhumedo.kmp.handball_support.ui
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton

import de.exhumedo.kmp.handball_support.ui.theme.AppTheme

import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
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
            DhbButton(onClick = onSelect) {
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
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
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingIndicatorTransition")

    // Single animation that cycles through 0-1 over 1200ms
    val progress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 1200, delayMillis = 0)
        ),
        label = "loadingProgress"
    ).value

    // Calculate alpha for each dot based on progress
    fun calculateAlpha(dotIndex: Int): Float {
        val phaseOffset = (dotIndex * 400f) / 1200f  // Each dot starts 400ms apart
        val phase = ((progress + phaseOffset) * 1f) % 1f

        return when {
            phase < 0.25f -> (phase / 0.25f).coerceIn(0f, 1f)           // Fade in 0-0.25
            phase < 0.5f -> 1f                        // Stay filled 0.25-0.5
            phase < 0.75f -> (1f - ((phase - 0.5f) / 0.25f)).coerceIn(0f, 1f)  // Fade out 0.5-0.75
            else -> 0f                                // Stay transparent 0.75-1
        }
    }

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw three dots arranged in a circle (120 degrees apart)
        repeat(3) { index ->
            val angle = ((index * 120f) * (PI / 180f))
            val radius = 12.dp

            val offsetX = (radius * cos(angle).toFloat())
            val offsetY = (radius * sin(angle).toFloat())

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = offsetX, y = offsetY)
                    .size(4.dp)  // radius 2 = diameter 4
                    .background(
                        color = color.copy(alpha = calculateAlpha(index)),
                        shape = CircleShape
                    ),
            )
        }
    }
}


@Preview
@Composable
private fun MatchRowPreviewUnselected() {
    AppTheme {
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
    AppTheme {
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
    AppTheme {
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
    AppTheme {
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
    AppTheme {
        Section(title = "Preview Section") {
            Text("Section body content")
        }
    }
}

@Preview
@Composable
private fun LoadingIndicatorPreview() {
    AppTheme {
        LoadingIndicator()
    }
}
