package de.exhumedo.kmp.handball_support.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbBlack
import de.exhumedo.kmp.handball_support.ui.theme.DhbRed
import de.exhumedo.kmp.handball_support.ui.theme.DhbYellow
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


/**
 * Defines a fill window for a single dot inside the cycle [0f, 1f].
 *
 * - The dot is empty (border only) before [fadeInStart].
 * - It fades in from [fadeInStart] to [fullStart].
 * - It stays fully filled from [fullStart] to [fadeOutStart].
 * - It fades back out from [fadeOutStart] to [fadeOutEnd].
 * - It is empty (border only) again after [fadeOutEnd].
 */
private data class DotPhase(
    val color: Color,
    val fadeInStart: Float,
    val fullStart: Float,
    val fadeOutStart: Float,
    val fadeOutEnd: Float,
)

private fun DotPhase.fillAlpha(progress: Float): Float = when {
    progress < fadeInStart -> 0f
    progress < fullStart -> ((progress - fadeInStart) / (fullStart - fadeInStart)).coerceIn(0f, 1f)
    progress < fadeOutStart -> 1f
    progress < fadeOutEnd -> (1f - (progress - fadeOutStart) / (fadeOutEnd - fadeOutStart)).coerceIn(0f, 1f)
    else -> 0f
}

@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    statusMessage: String = "Loading...",
    minDisplayDuration: Duration = 300.milliseconds,
) {
    // Persist the "is currently shown" flag across recompositions. We do NOT key
    // this on `isVisible` because that would reset the state every time the input
    // toggles and break the minimum-display-duration guarantee below.
    var shouldShowLoading by remember { mutableStateOf(isVisible) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Show immediately whenever the caller requests it.
            shouldShowLoading = true
        } else if (shouldShowLoading) {
            // Caller wants to hide it: keep it on screen for the configured
            // minimum duration before actually hiding.
            delay(minDisplayDuration)
            shouldShowLoading = false
        }
    }

    if (shouldShowLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.92f))
                // Announce loading state to screen readers and update them when
                // the message changes (e.g. "Signing in..." -> "Loading phases...").
                .semantics(mergeDescendants = true) {
                    contentDescription = statusMessage
                    liveRegion = LiveRegionMode.Polite
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LoadingDotsRow()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = statusMessage,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * Three circles (black, red, yellow) showing only their borders by default.
 * They fill sequentially - black first, then red, then yellow - and once all
 * three are filled they all clear back to borders, looping while displayed.
 */
@Composable
private fun LoadingDotsRow() {
    val transition = rememberInfiniteTransition(label = "loadingDotsCycle")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
        ),
        label = "loadingDotsProgress",
    )

    val phases = listOf(
        DotPhase(
            color = DhbBlack,
            fadeInStart = 0.00f,
            fullStart = 0.15f,
            fadeOutStart = 0.75f,
            fadeOutEnd = 0.90f,
        ),
        DotPhase(
            color = DhbRed,
            fadeInStart = 0.25f,
            fullStart = 0.40f,
            fadeOutStart = 0.75f,
            fadeOutEnd = 0.90f,
        ),
        DotPhase(
            color = DhbYellow,
            fadeInStart = 0.50f,
            fullStart = 0.65f,
            fadeOutStart = 0.75f,
            fadeOutEnd = 0.90f,
        ),
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        phases.forEach { phase ->
            val alpha = phase.fillAlpha(progress)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(width = 2.dp, color = phase.color, shape = CircleShape)
                    .background(
                        color = phase.color.copy(alpha = alpha),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun LoadingOverlayPreviewVisible() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Underlying screen content", modifier = Modifier.padding(16.dp))
            LoadingOverlay(
                isVisible = true,
                statusMessage = "Loading...",
            )
        }
    }
}

@Preview
@Composable
private fun LoadingOverlayPreviewSigningIn() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Underlying screen content", modifier = Modifier.padding(16.dp))
            LoadingOverlay(
                isVisible = true,
                statusMessage = "Signing in...",
            )
        }
    }
}

@Preview
@Composable
private fun LoadingOverlayPreviewLoadingPhases() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Underlying screen content", modifier = Modifier.padding(16.dp))
            LoadingOverlay(
                isVisible = true,
                statusMessage = "Loading phases...",
            )
        }
    }
}

@Preview
@Composable
private fun LoadingOverlayPreviewLongMessage() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Underlying screen content", modifier = Modifier.padding(16.dp))
            LoadingOverlay(
                isVisible = true,
                statusMessage = "Submitting your vote, please wait a moment...",
            )
        }
    }
}

@Preview
@Composable
private fun LoadingOverlayPreviewHidden() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Underlying screen content (overlay hidden)", modifier = Modifier.padding(16.dp))
            LoadingOverlay(
                isVisible = false,
                statusMessage = "Idle",
            )
        }
    }
}

