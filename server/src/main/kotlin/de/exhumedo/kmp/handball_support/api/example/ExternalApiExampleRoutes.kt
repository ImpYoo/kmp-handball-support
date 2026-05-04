package de.exhumedo.kmp.handball_support.api.example

import de.exhumedo.kmp.handball_support.application.ExternalApiException
import de.exhumedo.kmp.handball_support.application.ExternalApiGateway
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Example: API routes that use the Sportradar FMP Handball Datafeed API gateway.
 *
 * To use, pass the gateway to your routing function:
 *   configureSportradarApiRoutes(externalGateway)
 *
 * Environment setup:
 *   export EXTERNAL_API_ENABLED=true
 *   export EXTERNAL_API_BASE_URL=https://hbl.fmp.sportradar.com
 *   export EXTERNAL_API_KEY=your-sportradar-api-key
 *   export EXTERNAL_API_KEY_AS_QUERY_PARAM=true
 */
fun Application.configureSportradarApiRoutes(externalGateway: ExternalApiGateway) {
    routing {
        route("/api/sportradar") {
            /**
             * GET /api/sportradar/tournament
             *
             * Fetch all non-archived tournaments/leagues.
             */
            get("/tournament") {
                try {
                    val tournamentFeed = externalGateway.getTournament()
                    call.respond(
                        HttpStatusCode.OK,
                        SportradarFeedResponse(
                            meta = mapOf(
                                "generatedAt" to (tournamentFeed.meta.generatedAt ?: "unknown"),
                                "minCacheMillis" to (tournamentFeed.meta.minCacheMillis?.toString() ?: "0"),
                            ),
                            data = tournamentFeed.payload,
                        ),
                    )
                } catch (e: ExternalApiException) {
                    call.respond(
                        HttpStatusCode.BadGateway,
                        ErrorResponse("Failed to fetch tournaments: ${e.message}"),
                    )
                }
            }

            /**
             * GET /api/sportradar/standings/{phaseId}
             *
             * Fetch standings for a specific phase.
             */
            get("/standings/{phaseId}") {
                val phaseId = call.parameters["phaseId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Missing phaseId path parameter"),
                    )

                try {
                    val standingsFeed = externalGateway.getStandings(phaseId)
                    call.respond(
                        HttpStatusCode.OK,
                        SportradarFeedResponse(
                            meta = mapOf(
                                "generatedAt" to (standingsFeed.meta.generatedAt ?: "unknown"),
                                "minCacheMillis" to (standingsFeed.meta.minCacheMillis?.toString() ?: "0"),
                            ),
                            data = standingsFeed.payload,
                        ),
                    )
                } catch (e: ExternalApiException) {
                    call.respond(
                        HttpStatusCode.BadGateway,
                        ErrorResponse("Failed to fetch standings: ${e.message}"),
                    )
                }
            }

            /**
             * GET /api/sportradar/fixtures/{tournamentId}/{seasonId}
             *
             * Fetch all fixtures for a tournament and season.
             */
            get("/fixtures/{tournamentId}/{seasonId}") {
                val tournamentId = call.parameters["tournamentId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Missing tournamentId path parameter"),
                    )
                val seasonId = call.parameters["seasonId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Missing seasonId path parameter"),
                    )

                try {
                    val fixturesFeed = externalGateway.getFixtures(tournamentId, seasonId)
                    call.respond(
                        HttpStatusCode.OK,
                        SportradarFeedResponse(
                            meta = mapOf(
                                "generatedAt" to (fixturesFeed.meta.generatedAt ?: "unknown"),
                                "minCacheMillis" to (fixturesFeed.meta.minCacheMillis?.toString() ?: "0"),
                            ),
                            data = fixturesFeed.payload,
                        ),
                    )
                } catch (e: ExternalApiException) {
                    call.respond(
                        HttpStatusCode.BadGateway,
                        ErrorResponse("Failed to fetch fixtures: ${e.message}"),
                    )
                }
            }

            /**
             * GET /api/sportradar/team-info/{tournamentId}/{seasonId}/{teamId}
             *
             * Fetch detailed team information including squad, staff, and schedule.
             */
            get("/team-info/{tournamentId}/{seasonId}/{teamId}") {
                val tournamentId = call.parameters["tournamentId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Missing tournamentId path parameter"),
                    )
                val seasonId = call.parameters["seasonId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Missing seasonId path parameter"),
                    )
                val teamId = call.parameters["teamId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Missing teamId path parameter"),
                    )

                try {
                    val teamInfoFeed = externalGateway.getTeamInfo(tournamentId, seasonId, teamId)
                    call.respond(
                        HttpStatusCode.OK,
                        SportradarFeedResponse(
                            meta = mapOf(
                                "generatedAt" to (teamInfoFeed.meta.generatedAt ?: "unknown"),
                                "minCacheMillis" to (teamInfoFeed.meta.minCacheMillis?.toString() ?: "0"),
                            ),
                            data = teamInfoFeed.payload,
                        ),
                    )
                } catch (e: ExternalApiException) {
                    call.respond(
                        HttpStatusCode.BadGateway,
                        ErrorResponse("Failed to fetch team info: ${e.message}"),
                    )
                }
            }
        }
    }
}

@Serializable
data class SportradarFeedResponse(
    val meta: Map<String, String?>,
    val data: JsonObject,
)

@Serializable
data class ErrorResponse(
    val error: String,
)









