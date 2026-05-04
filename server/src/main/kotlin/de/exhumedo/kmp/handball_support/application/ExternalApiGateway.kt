package de.exhumedo.kmp.handball_support.application

import kotlinx.serialization.json.JsonObject

/**
 * Outbound port for external HTTP API reads.
 *
 * The server API layer can call this abstraction without depending on transport details.
 */
interface ExternalApiGateway {
    /**
     * Retrieves all non-archived tournaments/leagues.
     */
    suspend fun getTournament(): SportradarTournamentFeedDto

    /**
     * Retrieves standings for a specific phase.
     */
    suspend fun getStandings(phaseId: String): SportradarStandingsFeedDto

    /**
     * Retrieves fixtures for one tournament and one season.
     */
    suspend fun getFixtures(
        tournamentId: String,
        seasonId: String,
    ): SportradarFixturesFeedDto

    /**
     * Retrieves detailed information for one team in a tournament season.
     */
    suspend fun getTeamInfo(
        tournamentId: String,
        seasonId: String,
        teamId: String,
    ): SportradarTeamInfoFeedDto
}

data class SportradarFeedMetaDto(
    val generatedAt: String?,
    val minCacheMillis: Long?,
)

data class SportradarTournamentFeedDto(
    val meta: SportradarFeedMetaDto,
    val payload: JsonObject,
)

data class SportradarStandingsFeedDto(
    val meta: SportradarFeedMetaDto,
    val payload: JsonObject,
)

data class SportradarFixturesFeedDto(
    val meta: SportradarFeedMetaDto,
    val payload: JsonObject,
)

data class SportradarTeamInfoFeedDto(
    val meta: SportradarFeedMetaDto,
    val payload: JsonObject,
)

/**
 * Represents upstream or transport-related failures for outbound external API calls.
 */
class ExternalApiException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Default gateway used when outbound integration is disabled.
 */
object DisabledExternalApiGateway : ExternalApiGateway {
    private fun disabledError(methodName: String): Nothing {
        throw ExternalApiException(
            "External API integration is disabled. Cannot invoke '$methodName'.",
        )
    }

    override suspend fun getTournament(): SportradarTournamentFeedDto = disabledError("getTournament")

    override suspend fun getStandings(phaseId: String): SportradarStandingsFeedDto = disabledError("getStandings")

    override suspend fun getFixtures(tournamentId: String, seasonId: String): SportradarFixturesFeedDto =
        disabledError("getFixtures")

    override suspend fun getTeamInfo(
        tournamentId: String,
        seasonId: String,
        teamId: String,
    ): SportradarTeamInfoFeedDto = disabledError("getTeamInfo")
}


