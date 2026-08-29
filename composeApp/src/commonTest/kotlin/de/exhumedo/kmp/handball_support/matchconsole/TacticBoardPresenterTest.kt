package de.exhumedo.kmp.handball_support.matchconsole

import de.exhumedo.kmp.handball_support.persistence.InMemoryTacticStorage
import kotlin.test.Test
import kotlin.test.assertEquals

class TacticBoardPresenterTest {

    @Test
    fun savedPositionsAreRestoredOnConstruction() {
        val storage = InMemoryTacticStorage()
        val presenter = TacticBoardPresenter(storage)
        presenter.moveToken("h0", 5f, 2f)
        val moved = presenter.tokens.first { it.id == "h0" }

        // A fresh presenter (as after a page reload) must restore the moved position.
        val reloaded = TacticBoardPresenter(storage)
        val restored = reloaded.tokens.first { it.id == "h0" }
        assertEquals(moved.fieldX, restored.fieldX)
        assertEquals(moved.fieldY, restored.fieldY)
    }

    @Test
    fun moveTokenPersistsAcrossReload() {
        val storage = InMemoryTacticStorage()
        val presenter = TacticBoardPresenter(storage)
        presenter.moveToken("ball", 3f, -4f) // 20,10 -> 23,6
        val moved = presenter.tokens.first { it.id == "ball" }

        val reloaded = TacticBoardPresenter(storage)
        val restored = reloaded.tokens.first { it.id == "ball" }
        assertEquals(moved.fieldX, restored.fieldX)
        assertEquals(moved.fieldY, restored.fieldY)
    }

    @Test
    fun resetClearsStoredPositions() {
        val storage = InMemoryTacticStorage()
        val presenter = TacticBoardPresenter(storage)
        presenter.moveToken("ball", 3f, -4f)

        presenter.reset()

        // After reset a reload must start from the default formation again.
        val reloaded = TacticBoardPresenter(storage)
        val fresh = TacticBoardPresenter()
        assertEquals(fresh.tokens, reloaded.tokens)
    }

    @Test
    fun withoutStorageStartsFromDefaults() {
        val presenter = TacticBoardPresenter()
        val ball = presenter.tokens.first { it.id == "ball" }
        assertEquals(20f, ball.fieldX)
        assertEquals(10f, ball.fieldY)
    }
}