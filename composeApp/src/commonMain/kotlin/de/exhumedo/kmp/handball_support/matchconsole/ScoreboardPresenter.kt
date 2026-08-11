package de.exhumedo.kmp.handball_support.matchconsole

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State holder for a two-team scoreboard. Scores never go below zero.
 */
class ScoreboardPresenter {
    var homeScore by mutableStateOf(0)
        private set

    var guestScore by mutableStateOf(0)
        private set

    fun incrementHome() {
        homeScore++
    }

    fun decrementHome() {
        if (homeScore > 0) homeScore--
    }

    fun incrementGuest() {
        guestScore++
    }

    fun decrementGuest() {
        if (guestScore > 0) guestScore--
    }

    fun reset() {
        homeScore = 0
        guestScore = 0
    }

    /** Restores scores from persisted state. */
    fun restore(home: Int, guest: Int) {
        homeScore = home.coerceAtLeast(0)
        guestScore = guest.coerceAtLeast(0)
    }
}

