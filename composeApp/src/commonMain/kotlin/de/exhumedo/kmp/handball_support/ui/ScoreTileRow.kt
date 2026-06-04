package de.exhumedo.kmp.handball_support.ui

import de.exhumedo.kmp.handball_support.ui.theme.AppTheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Accessibility-first row of five square tiles for a 1..5 score (radio-button
 * style). Highlights:
 *
 * - Each tile is exposed as a [Role.RadioButton] inside a [selectableGroup],
 *   so TalkBack announces "1 of 5" / selected state correctly and arrow keys
 *   move focus between options.
 * - Tiles enforce a minimum 48 dp touch target (Android accessibility guideline).
 * - Color is paired with a shape (smiley curvature) so the meaning of each
 *   value is conveyed without relying on color alone (color-blind friendly).
 */
@Composable
fun ScoreTileRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val selectedScore = value.toIntOrNull()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..5).forEach { score ->
                ScoreTile(
                    score = score,
                    label = label,
                    selected = selectedScore == score,
                    enabled = enabled,
                    onClick = { onValueChange(score.toString()) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ScoreTile(
    score: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor = scoreColor(score)
    val shape = RoundedCornerShape(8.dp)

    val containerColor = if (selected) baseColor else MaterialTheme.colorScheme.surface
    val faceColor = if (selected) Color.White else baseColor
    val borderWidth = if (selected) 2.dp else 1.dp
    val contentAlpha = if (enabled) 1f else 0.4f
    val description = "$label: ${scoreSemanticLabel(score)} ($score of 5)"

    Box(
        modifier = modifier
            .aspectRatio(1f)
            // 48 dp minimum touch target per Android accessibility guidelines.
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .background(containerColor.copy(alpha = contentAlpha))
            .border(
                width = borderWidth,
                color = baseColor.copy(alpha = contentAlpha),
                shape = shape,
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        SmileyFace(
            score = score,
            color = faceColor.copy(alpha = contentAlpha),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Draws a smiley face on a square canvas. The mouth curvature varies with the
 * [score]: 1 = strong frown, 3 = neutral, 5 = strong smile.
 */
@Composable
private fun SmileyFace(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Eyes
        val eyeRadius = s * 0.07f
        val eyeOffsetX = s * 0.18f
        val eyeOffsetY = s * 0.14f
        drawCircle(
            color = color,
            radius = eyeRadius,
            center = Offset(cx - eyeOffsetX, cy - eyeOffsetY),
        )
        drawCircle(
            color = color,
            radius = eyeRadius,
            center = Offset(cx + eyeOffsetX, cy - eyeOffsetY),
        )

        // Mouth — quadratic Bezier whose control point Y depends on score.
        val mouthHalfWidth = s * 0.22f
        val mouthY = cy + s * 0.14f
        val mouthCurve = mouthCurveFor(score) * s * 0.30f
        val controlY = mouthY + mouthCurve

        val mouthPath = Path().apply {
            moveTo(cx - mouthHalfWidth, mouthY)
            quadraticBezierTo(
                cx, controlY,
                cx + mouthHalfWidth, mouthY,
            )
        }

        drawPath(
            path = mouthPath,
            color = color,
            style = Stroke(
                width = s * 0.07f,
                cap = StrokeCap.Round,
            ),
        )
    }
}

/**
 * Returns a value in [-1f, 1f]:
 *  - negative pulls the mouth control point upward (frown)
 *  - zero leaves the mouth straight (neutral)
 *  - positive pulls it downward (smile)
 */
private fun mouthCurveFor(score: Int): Float = when (score) {
    1 -> -1.0f
    2 -> -0.5f
    3 -> 0.0f
    4 -> 0.6f
    5 -> 1.0f
    else -> 0f
}

/** Maps a score in 1..5 to a color from red (worst) to dark green (best). */
private fun scoreColor(score: Int): Color = when (score) {
    1 -> Color(0xFFD32F2F) // red
    2 -> Color(0xFFF57C00) // orange
    3 -> Color(0xFFFBC02D) // amber / yellow
    4 -> Color(0xFF7CB342) // light green
    5 -> Color(0xFF2E7D32) // dark green
    else -> Color.Gray
}

/** Human-readable label for screen readers. */
private fun scoreSemanticLabel(score: Int): String = when (score) {
    1 -> "very negative"
    2 -> "negative"
    3 -> "neutral"
    4 -> "positive"
    5 -> "very positive"
    else -> "unknown"
}

@Preview
@Composable
private fun ScoreTileRowPreviewUnselected() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreTileRow(
                label = "Appearance",
                value = "",
                onValueChange = {},
            )
        }
    }
}

@Preview
@Composable
private fun ScoreTileRowPreviewSelectedLow() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreTileRow(
                label = "Appearance",
                value = "1",
                onValueChange = {},
            )
        }
    }
}

@Preview
@Composable
private fun ScoreTileRowPreviewSelectedHigh() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreTileRow(
                label = "Teamwork",
                value = "5",
                onValueChange = {},
            )
        }
    }
}

@Preview
@Composable
private fun ScoreTileRowPreviewDisabled() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreTileRow(
                label = "Influence (read-only)",
                value = "3",
                onValueChange = {},
                enabled = false,
            )
        }
    }
}


