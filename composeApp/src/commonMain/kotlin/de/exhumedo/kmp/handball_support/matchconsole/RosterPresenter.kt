package de.exhumedo.kmp.handball_support.matchconsole

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Which team a roster entry belongs to. */
enum class RosterTeam { HOME, GUEST }

/** A single roster entry. [number] is optional (kept as text to allow leading zeros / blanks). */
data class Player(
    val id: String,
    val number: String,
    val name: String,
)

/**
 * State holder for the home/guest team rosters. Players are kept sorted by
 * jersey number (numeric where possible, then by name).
 */
class RosterPresenter {
    var homePlayers by mutableStateOf<List<Player>>(emptyList())
        private set

    var guestPlayers by mutableStateOf<List<Player>>(emptyList())
        private set

    private var nextId = 0

    fun playersOf(team: RosterTeam): List<Player> = when (team) {
        RosterTeam.HOME -> homePlayers
        RosterTeam.GUEST -> guestPlayers
    }

    /** Adds a player to [team]. Returns false if the name is blank (nothing added). */
    fun addPlayer(team: RosterTeam, number: String, name: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return false
        val player = Player(id = "p${nextId++}", number = number.trim(), name = trimmedName)
        update(team) { it + player }
        return true
    }

    fun removePlayer(team: RosterTeam, playerId: String) {
        update(team) { list -> list.filterNot { it.id == playerId } }
    }

    fun reset() {
        homePlayers = emptyList()
        guestPlayers = emptyList()
    }

    /** Restores rosters from persisted state, preserving player ids. */
    fun restore(home: List<Player>, guest: List<Player>) {
        homePlayers = home
        guestPlayers = guest
        val maxId = (home + guest).mapNotNull { it.id.removePrefix("p").toIntOrNull() }.maxOrNull() ?: -1
        nextId = maxId + 1
    }

    private fun update(team: RosterTeam, transform: (List<Player>) -> List<Player>) {
        val sorted: (List<Player>) -> List<Player> = { list ->
            transform(list).sortedWith(
                compareBy({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.name }),
            )
        }
        when (team) {
            RosterTeam.HOME -> homePlayers = sorted(homePlayers)
            RosterTeam.GUEST -> guestPlayers = sorted(guestPlayers)
        }
    }
}

