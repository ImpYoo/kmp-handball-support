package de.exhumedo.kmp.handball_support.matchconsole

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State holder for the match setup: the two teams (name + short abbreviation)
 * and the names of the two referees.
 */
class MatchSetupPresenter {
    var homeTeamName by mutableStateOf("")
    var homeTeamAbbreviation by mutableStateOf("")

    var guestTeamName by mutableStateOf("")
    var guestTeamAbbreviation by mutableStateOf("")

    var firstRefereeName by mutableStateOf("")
    var secondRefereeName by mutableStateOf("")

    /** Optional external game identifier used to link a coaching evaluation to a match. */
    var gameId by mutableStateOf("")

    /** ISO-8601 date string (yyyy-MM-dd) of the match, used when uploading a coaching evaluation. */
    var matchDate by mutableStateOf("")

    fun reset() {
        homeTeamName = ""
        homeTeamAbbreviation = ""
        guestTeamName = ""
        guestTeamAbbreviation = ""
        firstRefereeName = ""
        secondRefereeName = ""
        gameId = ""
        matchDate = ""
    }

    companion object {
        /** Maximum length for a team abbreviation (e.g. "THW", "FCB"). */
        const val MAX_ABBREVIATION_LENGTH = 4
    }
}

