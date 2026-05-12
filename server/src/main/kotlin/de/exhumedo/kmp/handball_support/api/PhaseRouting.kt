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
            get {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                val from = call.request.queryParameters["from"]?.toLongOrNull() ?: 0L
                call.respond(HttpStatusCode.OK, matchApplicationService.getPhasesFromTimestamp(from).map { it.toResponseDto() })
            }
            get("/{phaseId}/matches") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                val phaseId = call.parameters["phaseId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "phaseId must be an integer")
                val from = call.request.queryParameters["from"]?.toLongOrNull() ?: 0L
                call.respond(HttpStatusCode.OK, matchApplicationService.getMatchesOfPhaseFromTimestamp(phaseId, from).map { it.toResponseDto() })
            }
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
