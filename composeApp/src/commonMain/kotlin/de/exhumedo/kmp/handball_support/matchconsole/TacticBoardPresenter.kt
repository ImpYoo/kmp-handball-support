package de.exhumedo.kmp.handball_support.matchconsole

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class TokenType { HOME, GUEST, BALL, REFEREE }

data class TacticToken(
    val id: String,
    val label: String,
    val type: TokenType,
    val fieldX: Float, // 0..40 (metres)
    val fieldY: Float, // 0..20 (metres)
)

/**
 * State holder for the tactic board. Token positions are stored in field
 * metres so the board scales correctly to any screen size.
 */
class TacticBoardPresenter {
    var tokens by mutableStateOf(defaultTokens())
        private set

    fun moveToken(id: String, deltaFieldX: Float, deltaFieldY: Float) {
        tokens = tokens.map { token ->
            if (token.id == id) token.copy(
                fieldX = (token.fieldX + deltaFieldX).coerceIn(0f, 40f),
                fieldY = (token.fieldY + deltaFieldY).coerceIn(0f, 20f),
            ) else token
        }
    }

    /** Moves the token to the end of the list so it renders on top while being dragged. */
    fun bringToFront(id: String) {
        val token = tokens.firstOrNull { it.id == id } ?: return
        tokens = tokens.filter { it.id != id } + token
    }

    fun reset() { tokens = defaultTokens() }

    private fun defaultTokens(): List<TacticToken> = buildList {
        // Home – left half, attacking right
        add(TacticToken("h0", "TW", TokenType.HOME,  1.5f, 10f))
        add(TacticToken("h1", "LA", TokenType.HOME, 12f,   5f))
        add(TacticToken("h2", "MI", TokenType.HOME, 14f,  10f))
        add(TacticToken("h3", "RA", TokenType.HOME, 12f,  15f))
        add(TacticToken("h4", "LL", TokenType.HOME,  8f,   2f))
        add(TacticToken("h5", "RL", TokenType.HOME,  8f,  18f))
        add(TacticToken("h6", "KR", TokenType.HOME, 17f,  10f))
        // Guest – right half, attacking left
        add(TacticToken("g0", "TW", TokenType.GUEST, 38.5f, 10f))
        add(TacticToken("g1", "LA", TokenType.GUEST, 28f,   5f))
        add(TacticToken("g2", "MI", TokenType.GUEST, 26f,  10f))
        add(TacticToken("g3", "RA", TokenType.GUEST, 28f,  15f))
        add(TacticToken("g4", "LL", TokenType.GUEST, 32f,   2f))
        add(TacticToken("g5", "RL", TokenType.GUEST, 32f,  18f))
        add(TacticToken("g6", "KR", TokenType.GUEST, 23f,  10f))
        // Ball
        add(TacticToken("ball", "", TokenType.BALL, 20f, 10f))
        // Referees
        add(TacticToken("A", "", TokenType.REFEREE, 0f, 4f))
        add(TacticToken("B", "", TokenType.REFEREE, 20f, 15f))
    }
}

