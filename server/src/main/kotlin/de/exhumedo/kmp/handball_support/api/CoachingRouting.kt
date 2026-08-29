package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.api.dto.coaching.toCatalogDto
import de.exhumedo.kmp.handball_support.api.dto.coaching.toCommand
import de.exhumedo.kmp.handball_support.api.dto.coaching.toResponseDto
import de.exhumedo.kmp.handball_support.application.coaching.CoachingApplicationService
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.authorize
import de.exhumedo.kmp.handball_support.security.verifiedTokenOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureCoachingRouting(
    coachingService: CoachingApplicationService,
    tokenService: JwtTokenService,
    authUserStore: AuthUserStore,
) {
    routing {
        route("/api/coaching") {

            // Public catalog
            get("/catalog") {
                val catalog = DefaultCriterionCatalog().loadCriteria().toCatalogDto()
                call.respond(HttpStatusCode.OK, catalog)
            }

            route("/evaluations") {

                post {
                    if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.COACH) == null) return@post
                    val request = call.receive<de.exhumedo.kmp.handball_support.api.dto.coaching.CreateCoachingEvaluationRequestDto>()
                    val actor = call.verifiedTokenOrNull()
                    if (actor?.role != AuthRole.ADMIN && actor?.subject != request.evaluatorUsername.trim()) {
                        call.respondProblem(HttpStatusCode.Forbidden, "Forbidden", "You may only create evaluations for yourself.")
                        return@post
                    }
                    val created = coachingService.create(request.toCommand())
                    call.respond(HttpStatusCode.Created, created.toResponseDto())
                }

                get {
                    if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.COACH, AuthRole.VIEWER) == null) return@get
                    val gameId = call.request.queryParameters["gameId"]
                    val evaluatorUsername = call.request.queryParameters["evaluatorUsername"]
                    val from = call.request.queryParameters["from"]
                    val to = call.request.queryParameters["to"]
                    val evaluations = coachingService.findAll(
                        filter = de.exhumedo.kmp.handball_support.persistence.coaching.CoachingEvaluationFilter(
                            gameId = gameId,
                            evaluatorUsername = evaluatorUsername,
                            from = from,
                            to = to,
                        )
                    )
                    call.respond(HttpStatusCode.OK, evaluations.map { it.toResponseDto() })
                }

                get("/{id}") {
                    if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.COACH, AuthRole.VIEWER) == null) return@get
                    val id = call.parameters["id"] ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "id is required")
                    val evaluation = coachingService.findById(id)
                    if (evaluation == null) {
                        call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Evaluation not found")
                    } else {
                        call.respond(HttpStatusCode.OK, evaluation.toResponseDto())
                    }
                }

                put("/{id}") {
                    if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.COACH) == null) return@put
                    val id = call.parameters["id"] ?: return@put call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "id is required")
                    val request = call.receive<de.exhumedo.kmp.handball_support.api.dto.coaching.CreateCoachingEvaluationRequestDto>()
                    val actor = call.verifiedTokenOrNull()
                    if (actor?.role != AuthRole.ADMIN && actor?.subject != request.evaluatorUsername.trim()) {
                        call.respondProblem(HttpStatusCode.Forbidden, "Forbidden", "You may only update your own evaluations.")
                        return@put
                    }
                    val updated = coachingService.update(id, request.toCommand())
                    if (updated == null) {
                        call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Evaluation not found")
                    } else {
                        call.respond(HttpStatusCode.OK, updated.toResponseDto())
                    }
                }

                get("/{id}/report") {
                    if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.COACH, AuthRole.VIEWER) == null) return@get
                    val id = call.parameters["id"] ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "id is required")
                    val report = coachingService.buildReport(id)
                    if (report == null) {
                        call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Evaluation not found")
                    } else {
                        call.respond(HttpStatusCode.OK, report.toResponseDto())
                    }
                }
            }
        }
    }
}
