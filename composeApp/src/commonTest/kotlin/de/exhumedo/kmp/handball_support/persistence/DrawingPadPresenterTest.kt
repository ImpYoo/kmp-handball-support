package de.exhumedo.kmp.handball_support.persistence

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawingPadPresenterTest {

    @Test
    fun restoreIntoNewPresenterReproducesStrokes() {
        val storage = InMemoryDrawingStorage()
        val presenter = DrawingPadPresenter(storage)
        presenter.addStroke(
            points = listOf(Offset(10f, 20f), Offset(30f, 40f)),
            color = Color.Black,
            strokeWidthDp = 4f,
        )

        val reloaded = DrawingPadPresenter(storage)
        assertEquals(1, reloaded.strokes.size)
        val stroke = reloaded.strokes.first()
        assertEquals(listOf(Offset(10f, 20f), Offset(30f, 40f)), stroke.points)
        assertEquals(Color.Black, stroke.color)
        assertEquals(4f, stroke.strokeWidthDp)
    }

    @Test
    fun colorRoundtripPreservesColor() {
        val storage = InMemoryDrawingStorage()
        val presenter = DrawingPadPresenter(storage)
        val colors = listOf(
            Color.Black,
            Color(0xFFF44336),
            Color(0xFF1565C0),
            Color(0xFF2E7D32),
            Color(0xFFEF6C00),
            Color.White,
        )
        colors.forEachIndexed { i, c ->
            presenter.addStroke(points = listOf(Offset(i.toFloat(), 0f)), color = c, strokeWidthDp = 2f)
        }

        val reloaded = DrawingPadPresenter(storage)
        assertEquals(colors.size, reloaded.strokes.size)
        colors.forEachIndexed { i, expected ->
            assertEquals(expected, reloaded.strokes[i].color, "color at index $i did not roundtrip")
        }
    }

    @Test
    fun undoingEveryStrokePersistsEmptyState() {
        val storage = InMemoryDrawingStorage()
        val presenter = DrawingPadPresenter(storage)
        presenter.addStroke(points = listOf(Offset(0f, 0f), Offset(5f, 5f)), color = Color.Black, strokeWidthDp = 4f)
        presenter.addStroke(points = listOf(Offset(1f, 1f), Offset(6f, 6f)), color = Color.Black, strokeWidthDp = 4f)

        presenter.removeLastStroke()
        presenter.removeLastStroke()

        // A fresh app start must not resurrect the removed strokes.
        val reloaded = DrawingPadPresenter(storage)
        assertTrue(reloaded.strokes.isEmpty(), "undoing all strokes must persist the empty state")
    }

    @Test
    fun clearWipesStorage() {
        val storage = InMemoryDrawingStorage()
        val presenter = DrawingPadPresenter(storage)
        presenter.addStroke(points = listOf(Offset(0f, 0f), Offset(5f, 5f)), color = Color.Black, strokeWidthDp = 4f)

        presenter.clear()

        val reloaded = DrawingPadPresenter(storage)
        assertTrue(reloaded.strokes.isEmpty())
    }
}