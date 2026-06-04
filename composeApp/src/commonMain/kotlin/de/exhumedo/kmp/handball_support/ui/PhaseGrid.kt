package de.exhumedo.kmp.handball_support.ui

import de.exhumedo.kmp.handball_support.ui.theme.AppTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import handball_support.composeapp.generated.resources.Res
import handball_support.composeapp.generated.resources.dhb_logo
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.client.PhaseResponseDto

/**
 * Responsive grid that lays out [phases] as square tiles. The number of columns
 * adapts to the available width:
 *
 * - phones (<600 dp wide): 2 columns
 * - small tablets (<900 dp): 4 columns
 * - large tablets (<1240 dp): 6 columns
 * - desktop (<1600 dp): 7 columns
 * - large desktop: 8 columns (capped)
 *
 * Tiles in the last (possibly partial) row keep their size — empty slots are
 * filled with invisible spacers so existing tiles don't grow.
 */
@Composable
fun PhaseGrid(
    phases: List<PhaseResponseDto>,
    selectedPhaseId: Int?,
    onPhaseClick: (PhaseResponseDto) -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = columnsFor(maxWidth)
        val rows = phases.chunked(columns)

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            rows.forEach { rowPhases ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowPhases.forEach { phase ->
                        PhaseTile(
                            phase = phase,
                            selected = phase.phaseId == selectedPhaseId,
                            onClick = { onPhaseClick(phase) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Pad the trailing row so tile size stays consistent.
                    repeat(columns - rowPhases.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun columnsFor(maxWidth: Dp): Int = when {
    maxWidth < 600.dp -> 2
    maxWidth < 900.dp -> 4
    maxWidth < 1240.dp -> 6
    maxWidth < 1600.dp -> 7
    else -> 8
}

/**
 * Square clickable tile with an image placeholder on top and the phase name
 * underneath. The whole tile reacts to clicks (no separate "Open" button).
 */
@Composable
fun PhaseTile(
    phase: PhaseResponseDto,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (selected) 2.dp else 1.dp
    val phaseDescription = buildString {
        append("Phase ")
        append(phase.name)
        if (selected) append(", currently selected")
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            // Ensure the whole tile meets the 48 dp minimum touch target.
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClickLabel = "Open phase ${phase.name}",
                onClick = onClick,
            )
            .semantics { contentDescription = phaseDescription },
        shape = shape,
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Image placeholder — DHB logo filling the upper portion of the tile.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.dhb_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                )
            }
            // Phase name under the image. Always reserves 2 lines so every tile
            // has the same height regardless of how short the name is.
            Text(
                text = phase.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun PhaseTilePreviewUnselected() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PhaseTile(
                phase = previewPhases.first(),
                selected = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun PhaseTilePreviewSelected() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PhaseTile(
                phase = previewPhases.first(),
                selected = true,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun PhaseGridPreviewSmall() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PhaseGrid(
                phases = previewPhases,
                selectedPhaseId = previewPhases.first().phaseId,
                onPhaseClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PhaseGridPreviewMany() {
    val many = List(10) { index ->
        previewPhases.first().copy(
            phaseId = 9000 + index,
            name = "DAIKIN HBL 2025/26 - Matchday ${index + 1}",
            shortName = "MD ${index + 1}",
        )
    }
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PhaseGrid(
                phases = many,
                selectedPhaseId = many[2].phaseId,
                onPhaseClick = {},
            )
        }
    }
}

