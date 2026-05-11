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

fun Application.configurePhaseRouting(
    matchApplicationService: MatchApplicationService,
    tokenService: JwtTokenService,
    authUserStore: AuthUserStore,
) {
    routing {
        route("/api/phases") {
            // Returns only phases that have at least one match within today and the 2 preceding days.
            get {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                call.respond(HttpStatusCode.OK, matchApplicationService.getPhases().map { it.toResponseDto() })
            }
            // Returns matches of a phase within the same 3-day window.
            get("/{phaseId}/matches") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                val phaseId = call.parameters["phaseId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "phaseId must be an integer")
                call.respond(HttpStatusCode.OK, matchApplicationService.getMatchesOfPhase(phaseId).map { it.toResponseDto() })
            }
            // Returns a single match by ID (no time filter — match ID is authoritative).
            get("/{phaseId}/matches/{matchId}") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                val phaseId = call.parameters["phaseId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "phaseId must be an integer")
                val matchId = call.parameters["matchId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "matchId must be an integer")
                val match = matchApplicationService.getMatch(phaseId, matchId)
                if (match == null) call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Match not found")
                else call.respond(HttpStatusCode.OK, match.toResponseDto())
            }
        }
    }
}
