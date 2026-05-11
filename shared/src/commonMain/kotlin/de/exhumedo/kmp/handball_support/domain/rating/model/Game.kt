package de.exhumedo.kmp.handball_support.domain.rating.model

data class Game(
    val gameId: String,
    val date: String,
    val homeTeam: String,
    val awayTeam: String,
    val venue: String,
)
