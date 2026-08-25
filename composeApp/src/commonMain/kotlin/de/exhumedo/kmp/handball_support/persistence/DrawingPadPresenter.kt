package de.exhumedo.kmp.handball_support.persistence

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Presenter that owns the drawing pad strokes and persists them via [DrawingStorage].
 * It lives at the App level so strokes survive navigation away/back.
 */
class DrawingPadPresenter(storage: DrawingStorage) {

    private val _strokes: SnapshotStateList<DrawnStroke> = mutableStateListOf()
    val strokes: List<DrawnStroke> get() = _strokes

    init {
        storage.read()?.deserializeStrokes()?.let { saved ->
            _strokes.addAll(saved)
        }
    }

    private var storage: DrawingStorage? = storage

    fun addStroke(points: List<Offset>, color: Color, strokeWidthDp: Float) {
        _strokes.add(DrawnStroke(points, color, strokeWidthDp))
        persist()
    }

    fun removeLastStroke() {
        if (_strokes.isNotEmpty()) {
            _strokes.removeAt(_strokes.lastIndex)
            persist()
        }
    }

    fun clear() {
        _strokes.clear()
        storage?.clear()
    }

    private fun persist() {
        if (_strokes.isNotEmpty()) {
            storage?.save(_strokes.toList().serialize())
        }
    }
}

/**
 * Single drawing stroke.
 */
data class DrawnStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidthDp: Float,
)

@Serializable
private data class SerializablePoint(val x: Float, val y: Float)

@Serializable
private data class SerializableStroke(
    val points: List<SerializablePoint>,
    val color: Long,
    val strokeWidthDp: Float,
)

@Serializable
private data class SerializableStrokeList(val strokes: List<SerializableStroke>)

private fun DrawnStroke.toSerializable(): SerializableStroke =
    SerializableStroke(points.map { SerializablePoint(it.x, it.y) }, color.value.toLong(), strokeWidthDp)

private fun SerializableStroke.toDrawnStroke(): DrawnStroke =
    DrawnStroke(points.map { Offset(it.x, it.y) }, Color(color), strokeWidthDp)

private fun List<DrawnStroke>.serialize(): String =
    Json.encodeToString(SerializableStrokeList.serializer(), SerializableStrokeList(map { it.toSerializable() }))

internal fun String.deserializeStrokes(): List<DrawnStroke> =
    runCatching { Json.decodeFromString(SerializableStrokeList.serializer(), this).strokes.map { it.toDrawnStroke() } }.getOrElse { emptyList() }
