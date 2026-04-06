package de.exhumedo.kmp.handball_support.domain.rating.model

/**
 * Immutable reference to a game owned by an external federation system.
 *
 * This domain does not manage the game lifecycle. It only stores enough
 * identifying information to link one evaluation to one external game.
 * Strings are used for date values to keep the common domain model free from
 * additional date/time library dependencies across KMP targets.
 *
 * @property gameId External federation identifier for the game.
 * @property date ISO 8601 calendar date string such as `2025-03-15`.
 * @property homeTeam Home team name.
 * @property awayTeam Away team name.
 * @property venue Venue name for the game.
 */
data class Game(
    val gameId: String,
    val date: String,
    val homeTeam: String,
    val awayTeam: String,
    val venue: String,
) {
    init {
        require(gameId.isNotBlank()) { "Game gameId must not be blank" }
        require(date.isNotBlank()) { "Game date must not be blank" }
        require(homeTeam.isNotBlank()) { "Game homeTeam must not be blank" }
        require(awayTeam.isNotBlank()) { "Game awayTeam must not be blank" }
        require(venue.isNotBlank()) { "Game venue must not be blank" }
    }
}
