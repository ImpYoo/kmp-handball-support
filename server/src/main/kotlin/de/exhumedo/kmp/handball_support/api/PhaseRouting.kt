package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.api.dto.toResponseDto
import de.exhumedo.kmp.handball_support.application.MatchApplicationService
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.authorize
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Application.configurePhaseRouting(
    matchApplicationService: MatchApplicationService,
    tokenService: JwtTokenService,
    authUserStore: AuthUserStore,
) {
    routing {
        route("/api/phases") {

            // Returns phases with matches in the server-computed 3-day window.
            // No authentication required - users can browse phases freely.
            // Uses withContext(Dispatchers.IO) to prevent blocking Ktor's coroutine dispatcher
            // while the repository performs a (potentially blocking) Sportradar HTTP call.
            get {
                val day = call.request.queryParameters["day"]?.toIntOrNull()
                val month = call.request.queryParameters["month"]?.toIntOrNull()
                val year = call.request.queryParameters["year"]?.toIntOrNull()
                val phases = withContext(Dispatchers.IO) {
                    matchApplicationService.getPhases(day = day, month = month, year = year)
                }
                call.respond(HttpStatusCode.OK, phases.map { it.toResponseDto() })
            }

            get("/{phaseId}/matches") {
                val phaseId = call.parameters["phaseId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "phaseId must be an integer")
                val day = call.request.queryParameters["day"]?.toIntOrNull()
                val month = call.request.queryParameters["month"]?.toIntOrNull()
                val year = call.request.queryParameters["year"]?.toIntOrNull()
                val matches = withContext(Dispatchers.IO) {
                    matchApplicationService.getMatchesOfPhase(phaseId = phaseId, day = day, month = month, year = year)
                }
                call.respond(HttpStatusCode.OK, matches.map { it.toResponseDto() })
            }

            get("/{phaseId}/matches/{matchId}") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                val phaseId = call.parameters["phaseId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "phaseId must be an integer")
                val matchId = call.parameters["matchId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "matchId must be an integer")
                val match = withContext(Dispatchers.IO) { matchApplicationService.getMatch(phaseId, matchId) }
                if (match == null) call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Match not found")
                else call.respond(HttpStatusCode.OK, match.toResponseDto())
            }
        }
    }
}
