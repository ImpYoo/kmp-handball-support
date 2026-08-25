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
        // Home – 6-0 defense on 6m arc
        add(TacticToken("h0", "TW", TokenType.HOME,  1.5f, 10.0f))
        add(TacticToken("h5", "1",  TokenType.HOME,  1.3262209f, 18.230385f))
        add(TacticToken("h4", "2",  TokenType.HOME,  5.4000006f, 15.767547f))
        add(TacticToken("h3", "3",  TokenType.HOME,  6.8887196f, 11.548767f))
        add(TacticToken("h2", "4",  TokenType.HOME,  6.916339f,   8.378257f))
        add(TacticToken("h1", "5",  TokenType.HOME,  5.0866833f,  3.762579f))
        add(TacticToken("h6", "6",  TokenType.HOME,  1.2235881f,  1.6758004f))
        // Guest – attacking on 9m arc, mirrored names
        add(TacticToken("g0", "TW", TokenType.GUEST, 38.5f, 10.0f))
        add(TacticToken("g3", "LA", TokenType.GUEST,  6.8947606f, 18.956158f))
        add(TacticToken("g1", "RA", TokenType.GUEST,  6.7433944f,  0.99583316f))
        add(TacticToken("g2", "RM", TokenType.GUEST, 12.283315f,   9.829164f))
        add(TacticToken("g6", "KR", TokenType.GUEST,  7.7708316f,  9.962512f))
        add(TacticToken("g5", "RL", TokenType.GUEST, 10.750196f,  15.009657f))
        add(TacticToken("g4", "RR", TokenType.GUEST, 10.750834f,   5.2133274f))
        // Ball
        add(TacticToken("ball", "", TokenType.BALL, 20f, 10f))
        // Referees
        add(TacticToken("A", "", TokenType.REFEREE, 0f, 4f))
        add(TacticToken("B", "", TokenType.REFEREE, 20f, 15f))
    }
}

