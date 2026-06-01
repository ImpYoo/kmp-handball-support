package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.api.dto.SportradarFetchErrorDto
import de.exhumedo.kmp.handball_support.api.dto.SportradarRefreshDto
import de.exhumedo.kmp.handball_support.api.dto.SportradarSeasonDetailDto
import de.exhumedo.kmp.handball_support.api.dto.toDetailDto
import de.exhumedo.kmp.handball_support.api.dto.toErrorDto
import de.exhumedo.kmp.handball_support.api.dto.toMatchDetailDto
import de.exhumedo.kmp.handball_support.api.dto.toSeasonDto
import de.exhumedo.kmp.handball_support.api.dto.toSummaryDto
import de.exhumedo.kmp.handball_support.api.dto.toTournamentDto
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.authorize
import de.exhumedo.kmp.handball_support.sportradar.error.SportradarResult
import de.exhumedo.kmp.handball_support.sportradar.repository.SportradarPhaseRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

// ── Sportradar diagnostic routes ──────────────────────────────────────────────
// All endpoints are ADMIN-only — they expose raw Sportradar data and are meant
// for integration testing / verifying the HTTP client with real credentials.
//
//   GET /api/sportradar/phases                          → phase summaries (fixtures feed)
//   GET /api/sportradar/phases/{phaseId}                → full phase + match days
//   GET /api/sportradar/phases/{phaseId}/matches        → flat match list
//   GET /api/sportradar/phases/{phaseId}/matches/{id}   → single match
//   GET /api/sportradar/tournaments                     → all tournaments (tournament feed)
//   GET /api/sportradar/tournaments/{id}                → single tournament
//   GET /api/sportradar/tournaments/{id}/seasons        → seasons of a tournament
//   GET /api/sportradar/tournaments/{id}/seasons/{sid}  → season detail + fixture phases
//   POST /api/sportradar/refresh                        → clear and warm Sportradar caches
// ─────────────────────────────────────────────────────────────────────────────

private fun disabled() = SportradarFetchErrorDto("DISABLED", "Sportradar integration is disabled (EXTERNAL_API_ENABLED=false)")
private val sportradarRouteLog = LoggerFactory.getLogger("de.exhumedo.kmp.handball_support.api.SportradarRouting")

fun Application.configureSportradarRouting(
    sportradarRepo: SportradarPhaseRepository?,
    tokenService: JwtTokenService,
    authUserStore: AuthUserStore,
) {
    routing {
        route("/api/sportradar") {

            // ── GET /api/sportradar/phases ────────────────────────────────────
            get("/phases") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                if (sportradarRepo == null) { call.respond(HttpStatusCode.ServiceUnavailable, disabled()); return@get }

                when (val r = sportradarRepo.getAllPhases()) {
                    is SportradarResult.Success -> call.respond(HttpStatusCode.OK, r.value.map { it.toSummaryDto() })
                    is SportradarResult.Failure -> call.respond(HttpStatusCode.BadGateway, r.error.toErrorDto())
                }
            }

            // ── POST /api/sportradar/refresh ──────────────────────────────────
            // Clears all Sportradar caches and eagerly reloads tournament + fixture data.
            post("/refresh") {
                val token = call.authorize(tokenService, authUserStore, AuthRole.ADMIN) ?: return@post
                if (sportradarRepo == null) { call.respond(HttpStatusCode.ServiceUnavailable, disabled()); return@post }

                when (val r = sportradarRepo.refreshCaches()) {
                    is SportradarResult.Success -> {
                        sportradarRouteLog.info(
                            "Sportradar cache refresh triggered by user='{}': tournaments={}, configuredSeasons={}, phases={}",
                            token.subject,
                            r.value.tournamentCount,
                            r.value.configuredSeasonCount,
                            r.value.phaseCount,
                        )
                        call.respond(
                            HttpStatusCode.OK,
                            SportradarRefreshDto(
                                tournamentCount = r.value.tournamentCount,
                                configuredSeasonCount = r.value.configuredSeasonCount,
                                phaseCount = r.value.phaseCount,
                                refreshedAtEpochMillis = System.currentTimeMillis(),
                            ),
                        )
                    }
                    is SportradarResult.Failure -> call.respond(HttpStatusCode.BadGateway, r.error.toErrorDto())
                }
            }

            // ── GET /api/sportradar/phases/{phaseId} ──────────────────────────
            get("/phases/{phaseId}") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                if (sportradarRepo == null) { call.respond(HttpStatusCode.ServiceUnavailable, disabled()); return@get }

                val phaseId = call.parameters["phaseId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Bad Request", "phaseId must be an integer")

                when (val r = sportradarRepo.getAllPhases()) {
                    is SportradarResult.Success -> {
                        val phase = r.value.find { it.phaseId == phaseId }
                        if (phase == null) call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Phase $phaseId not found")
                        else call.respond(HttpStatusCode.OK, phase.toDetailDto())
                    }
                    is SportradarResult.Failure -> call.respond(HttpStatusCode.BadGateway, r.error.toErrorDto())
                }
            }

            // ── GET /api/sportradar/phases/{phaseId}/matches ──────────────────
            get("/phases/{phaseId}/matches") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                if (sportradarRepo == null) { call.respond(HttpStatusCode.ServiceUnavailable, disabled()); return@get }

                val phaseId = call.parameters["phaseId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Bad Request", "phaseId must be an integer")

                when (val r = sportradarRepo.getAllPhases()) {
                    is SportradarResult.Success -> {
                        val phase = r.value.find { it.phaseId == phaseId }
                        if (phase == null) call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Phase $phaseId not found")
                        else call.respond(HttpStatusCode.OK,
                            phase.matchDays.flatMap { md -> md.matches.map { it.toMatchDetailDto(md.id) } })
                    }
                    is SportradarResult.Failure -> call.respond(HttpStatusCode.BadGateway, r.error.toErrorDto())
                }
            }

            // ── GET /api/sportradar/phases/{phaseId}/matches/{matchId} ────────
            get("/phases/{phaseId}/matches/{matchId}") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                if (sportradarRepo == null) { call.respond(HttpStatusCode.ServiceUnavailable, disabled()); return@get }

                val phaseId = call.parameters["phaseId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Bad Request", "phaseId must be an integer")
                val matchId = call.parameters["matchId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Bad Request", "matchId must be an integer")

                when (val r = sportradarRepo.getAllPhases()) {
                    is SportradarResult.Success -> {
                        val phase = r.value.find { it.phaseId == phaseId }
                        if (phase == null) { call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Phase $phaseId not found"); return@get }
                        val matchDay = phase.matchDays.firstOrNull { md -> md.matches.any { it.id == matchId } }
                        val match = matchDay?.matches?.find { it.id == matchId }
                        if (match == null) call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Match $matchId not found in phase $phaseId")
                        else call.respond(HttpStatusCode.OK, match.toMatchDetailDto(matchDay.id))
                    }
                    is SportradarResult.Failure -> call.respond(HttpStatusCode.BadGateway, r.error.toErrorDto())
                }
            }

            // ── GET /api/sportradar/tournaments ───────────────────────────────
            // Calls the real Sportradar /tournament feed — all non-archived tournaments.
            get("/tournaments") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                if (sportradarRepo == null) { call.respond(HttpStatusCode.ServiceUnavailable, disabled()); return@get }

                when (val r = sportradarRepo.getTournaments()) {
                    is SportradarResult.Success -> call.respond(HttpStatusCode.OK, r.value.map { it.toTournamentDto() })
                    is SportradarResult.Failure -> call.respond(HttpStatusCode.BadGateway, r.error.toErrorDto())
                }
            }

            // ── GET /api/sportradar/tournaments/{tournamentId} ────────────────
            get("/tournaments/{tournamentId}") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                if (sportradarRepo == null) { call.respond(HttpStatusCode.ServiceUnavailable, disabled()); return@get }

                val tournamentId = call.parameters["tournamentId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Bad Request", "tournamentId must be an integer")

                when (val r = sportradarRepo.getTournaments()) {
                    is SportradarResult.Success -> {
                        val t = r.value.find { it.id == tournamentId }
                            ?: return@get call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Tournament $tournamentId not found")
                        call.respond(HttpStatusCode.OK, t.toTournamentDto())
                    }
                    is SportradarResult.Failure -> call.respond(HttpStatusCode.BadGateway, r.error.toErrorDto())
                }
            }

            // ── GET /api/sportradar/tournaments/{tournamentId}/seasons ────────
            get("/tournaments/{tournamentId}/seasons") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                if (sportradarRepo == null) { call.respond(HttpStatusCode.ServiceUnavailable, disabled()); return@get }

                val tournamentId = call.parameters["tournamentId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Bad Request", "tournamentId must be an integer")

                when (val r = sportradarRepo.getTournaments()) {
                    is SportradarResult.Success -> {
                        val t = r.value.find { it.id == tournamentId }
                            ?: return@get call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Tournament $tournamentId not found")
                        call.respond(HttpStatusCode.OK, t.seasons.map { it.toSeasonDto() })
                    }
                    is SportradarResult.Failure -> call.respond(HttpStatusCode.BadGateway, r.error.toErrorDto())
                }
            }

            // ── GET /api/sportradar/tournaments/{tournamentId}/seasons/{seasonId}
            // Tournament feed provides name/year/status; fixtures feed provides phase+match counts.
            get("/tournaments/{tournamentId}/seasons/{seasonId}") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                if (sportradarRepo == null) { call.respond(HttpStatusCode.ServiceUnavailable, disabled()); return@get }

                val tournamentId = call.parameters["tournamentId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Bad Request", "tournamentId must be an integer")
                val seasonId = call.parameters["seasonId"]?.toIntOrNull()
                    ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Bad Request", "seasonId must be an integer")

                // Step 1: resolve season metadata from tournament feed
                val tr = sportradarRepo.getTournaments()
                if (tr is SportradarResult.Failure) { call.respond(HttpStatusCode.BadGateway, tr.error.toErrorDto()); return@get }
                val season = (tr as SportradarResult.Success).value
                    .find { it.id == tournamentId }?.seasons?.find { it.id == seasonId }
                    ?: return@get call.respondProblem(HttpStatusCode.NotFound, "Not Found",
                        "Season $seasonId not found in tournament $tournamentId")

                // Step 2: enrich with fixture-feed phase summaries (if season is in local config)
                val phases = when (val fr = sportradarRepo.getPhasesForSeason(tournamentId, seasonId)) {
                    is SportradarResult.Success -> fr.value.map { it.toSummaryDto() }
                    is SportradarResult.Failure -> emptyList() // season not in local config — no match data
                }

                call.respond(HttpStatusCode.OK, SportradarSeasonDetailDto(
                    tournamentId = tournamentId,
                    seasonId     = season.id,
                    name         = season.name,
                    year         = season.year,
                    status       = season.status,
                    phases       = phases,
                ))
            }
        }
    }
}
