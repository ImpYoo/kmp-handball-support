package de.exhumedo.kmp.handball_support.sportradar.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Internal Sportradar API response DTOs ────────────────────────────────────
// Visibility is internal: callers always receive mapped domain objects.
// All lists/strings default to empty so unknown/missing fields never crash.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
internal data class SportradarFixturesResponse(
    val doc: List<SportradarDocEntry> = emptyList(),
)

@Serializable
internal data class SportradarDocEntry(
    val data: SportradarDocData? = null,
)

@Serializable
internal data class SportradarDocData(
    val tournament: SportradarTournament? = null,
)

@Serializable
internal data class SportradarTournament(
    val phases: List<SportradarPhase> = emptyList(),
)

@Serializable
internal data class SportradarPhase(
    @SerialName("_id") val id: Int,
    val name: String = "",
    val matchdays: List<SportradarMatchDay> = emptyList(),
)

@Serializable
internal data class SportradarMatchDay(
    @SerialName("_id") val id: Int,
    @SerialName("current_matchday") val currentMatchday: Boolean = false,
    val title: String = "",
    @SerialName("start_date") val startDate: SportradarTimestamp = SportradarTimestamp(0),
    @SerialName("end_date") val endDate: SportradarTimestamp = SportradarTimestamp(0),
    val matches: List<SportradarMatch> = emptyList(),
)

@Serializable
internal data class SportradarTimestamp(val uts: Long)

@Serializable
internal data class SportradarMatch(
    @SerialName("_id") val id: Int,
    @SerialName("play_date") val playDate: SportradarTimestamp,
    val location: SportradarLocation? = null,
    @SerialName("home_team") val homeTeam: SportradarTeam,
    @SerialName("away_team") val awayTeam: SportradarTeam,
    val referees: List<SportradarOfficial> = emptyList(),
)

@Serializable
internal data class SportradarLocation(
    @SerialName("_id") val id: Int,
    val street: String = "",
    val zip: String = "",
    val city: String = "",
)

@Serializable
internal data class SportradarTeam(
    @SerialName("_id") val id: Int,
    val name: String = "",
    @SerialName("final_result") val finalResult: String = "",
    @SerialName("result_first_half") val resultFirstHalf: String = "",
)

/** Covers referees, timekeepers, secretaries, delegates — all use same structure */
@Serializable
internal data class SportradarOfficial(
    @SerialName("_id") val id: Int,
    val name: String = "",
    @SerialName("type_key") val typeKey: String = "",
)

// ── Tournament list feed (/tournaments) ──────────────────────────────────────

@Serializable
internal data class SportradarTournamentListResponse(
    val doc: List<SportradarTournamentDocEntry> = emptyList(),
)

@Serializable
internal data class SportradarTournamentDocEntry(
    val data: SportradarTournamentData? = null,
)

@Serializable
internal data class SportradarTournamentData(
    // JSON key is "tournaments" (plural)
    val tournaments: List<SportradarTournamentEntry> = emptyList(),
)

@Serializable
internal data class SportradarTournamentEntry(
    @SerialName("_id") val id: Int = 0,
    val name: String = "",
    val seasons: List<SportradarSeasonEntry> = emptyList(),
)

@Serializable
internal data class SportradarSeasonEntry(
    @SerialName("_id") val id: Int = 0,
    val name: String = "",
    val year: String = "",
    val status: String = "",
    val phases: List<SportradarPhaseEntry> = emptyList(),
)

@Serializable
internal data class SportradarPhaseEntry(
    @SerialName("_id") val id: Int = 0,
    val name: String = "",
    // start_date / end_date are timestamp objects identical to SportradarTimestamp
    @SerialName("start_date") val startDate: SportradarTimestamp? = null,
    @SerialName("end_date")   val endDate: SportradarTimestamp? = null,
)

