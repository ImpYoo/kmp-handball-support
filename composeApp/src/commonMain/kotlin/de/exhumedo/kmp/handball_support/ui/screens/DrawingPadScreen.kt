package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import de.exhumedo.kmp.handball_support.persistence.DrawingStorage
import de.exhumedo.kmp.handball_support.persistence.drawingStorage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.DhbRed
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

private data class DrawnStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidthDp: Float,
)

private val PaletteColors = listOf(
    Color.Black,
    DhbRed,
    Color(0xFF1565C0), // blue
    Color(0xFF2E7D32), // green
    Color(0xFFEF6C00), // orange
    Color.White,
)


private fun DrawnStroke.toSerializable(): SerializableStroke =
    SerializableStroke(points.map { SerializablePoint(it.x, it.y) }, color.value.toLong(), strokeWidthDp)

private fun SerializableStroke.toDrawnStroke(): DrawnStroke =
    DrawnStroke(points.map { Offset(it.x, it.y) }, Color(color), strokeWidthDp)

private fun List<DrawnStroke>.serialize(): String =
    kotlinx.serialization.json.Json.encodeToString(SerializableStrokeList.serializer(), SerializableStrokeList(map { it.toSerializable() }))

private fun String.deserializeStrokes(): List<DrawnStroke> =
    runCatching { kotlinx.serialization.json.Json.decodeFromString(SerializableStrokeList.serializer(), this).strokes.map { it.toDrawnStroke() } }.getOrElse { emptyList() }

/**
 * Full-screen drawing pad: draw with finger/mouse, choose color and stroke width,
 * undo the last stroke, and clear everything.
 */
@Composable
fun DrawingPadScreen(
    storage: DrawingStorage = drawingStorage(),
    onNavigateHome: () -> Unit,
) {
    val strokes = remember { mutableStateListOf<DrawnStroke>() }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(4f) }

    // Load saved strokes on first composition.
    DisposableEffect(Unit) {
        storage.read()?.let { strokes.addAll(it.deserializeStrokes()) }
        onDispose { }
    }

    // Persist whenever strokes change (new stroke, undo, clear will trigger this).
    DisposableEffect(strokes.toList()) {
        if (strokes.isNotEmpty()) storage.save(strokes.toList().serialize()) else storage.clear()
        onDispose { }
    }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Notizblock",
                subtitle = "Zeichenfläche",
                onLogoClick = onNavigateHome,
                actions = {
                    DhbButton(onClick = onNavigateHome) { Text("Menü") }
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = Dimens.contentMaxWidth)
                        .fillMaxHeight()
                        .padding(Dimens.spaceLg),
                ) {
                    // --- Toolbar ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                    ) {
                        // Undo
                        DhbButton(
                            onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                            enabled = strokes.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = "Rückgängig")
                        }
                        // Clear all
                        DhbButton(
                            onClick = {
                                strokes.clear()
                                storage.clear()
                            },
                            enabled = strokes.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Alles löschen")
                        }

                        Spacer(Modifier.width(Dimens.spaceSm))

                        // Color palette
                        PaletteColors.forEach { swatchColor ->
                            val selected = swatchColor == currentColor
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 34.dp else 28.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) DhbRed else Color.Gray,
                                        shape = CircleShape,
                                    )
                                    .pointerInput(swatchColor) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            currentColor = swatchColor
                                        }
                                    },
                            )
                        }

                        Spacer(Modifier.width(Dimens.spaceSm))

                        // Stroke-width slider
                        Text(
                            text = "${strokeWidth.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 2f..20f,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(Dimens.spaceSm))

                    // --- Canvas ---
                    val currentPoints = remember { mutableStateListOf<Offset>() }
                    // rememberUpdatedState ensures the pointerInput lambda always
                    // reads the latest color/width even though the lambda key is Unit.
                    val colorState = rememberUpdatedState(currentColor)
                    val widthState = rememberUpdatedState(strokeWidth)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(Dimens.cardElevation, RoundedCornerShape(Dimens.cardCorner))
                            .clip(RoundedCornerShape(Dimens.cardCorner))
                            .background(Color.White)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    currentPoints.clear()
                                    currentPoints.add(down.position)
                                    do {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { change ->
                                            change.consume()
                                            currentPoints.add(change.position)
                                        }
                                    } while (event.changes.any { it.pressed })
                                    if (currentPoints.isNotEmpty()) {
                                        strokes.add(
                                            DrawnStroke(
                                                points = currentPoints.toList(),
                                                color = colorState.value,
                                                strokeWidthDp = widthState.value,
                                            ),
                                        )
                                        currentPoints.clear()
                                    }
                                }
                            },
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Render all completed strokes
                            strokes.forEach { stroke ->
                                if (stroke.points.size < 2) return@forEach
                                val path = Path().apply {
                                    moveTo(stroke.points[0].x, stroke.points[0].y)
                                    stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(
                                    path = path,
                                    color = stroke.color,
                                    style = Stroke(
                                        width = stroke.strokeWidthDp.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                    ),
                                )
                            }
                            // Render the currently-active stroke in real time
                            if (currentPoints.size >= 2) {
                                val livePath = Path().apply {
                                    moveTo(currentPoints[0].x, currentPoints[0].y)
                                    currentPoints.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(
                                    path = livePath,
                                    color = colorState.value,
                                    style = Stroke(
                                        width = widthState.value.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DrawingPadScreenPreview() {
    DrawingPadScreen(storage = de.exhumedo.kmp.handball_support.persistence.InMemoryDrawingStorage(), onNavigateHome = {})
}

@kotlinx.serialization.Serializable
private data class SerializablePoint(val x: Float, val y: Float)

@kotlinx.serialization.Serializable
private data class SerializableStroke(val points: List<SerializablePoint>, val color: Long, val strokeWidthDp: Float)

@kotlinx.serialization.Serializable
private data class SerializableStrokeList(val strokes: List<SerializableStroke>)




