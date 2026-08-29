package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.api.dto.coaching.toCatalogDto
import de.exhumedo.kmp.handball_support.api.dto.coaching.toCommand
import de.exhumedo.kmp.handball_support.api.dto.coaching.toResponseDto
import de.exhumedo.kmp.handball_support.application.coaching.CoachingApplicationService
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.persistence.coaching.CoachingEvaluationFilter
import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.authorize
import de.exhumedo.kmp.handball_support.security.verifiedTokenOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
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
                    if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE_COACH, AuthRole.REFEREE_COACH_ADMIN) == null) return@post
                    val request = call.receive<de.exhumedo.kmp.handball_support.api.dto.coaching.CreateCoachingEvaluationRequestDto>()
                    val actor = call.verifiedTokenOrNull()
                    if (actor?.role != AuthRole.ADMIN && actor?.role != AuthRole.REFEREE_COACH_ADMIN && actor?.subject != request.evaluatorUsername.trim()) {
                        call.respondProblem(HttpStatusCode.Forbidden, "Forbidden", "You may only create evaluations for yourself.")
                        return@post
                    }
                    val created = coachingService.create(request.toCommand())
                    call.respond(HttpStatusCode.Created, created.toResponseDto())
                }

                get("/mine") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE_COACH, AuthRole.REFEREE_COACH_ADMIN) ?: return@get
                    val gameId = call.request.queryParameters["gameId"]
                    val from = call.request.queryParameters["from"]
                    val to = call.request.queryParameters["to"]
                    val filter = CoachingEvaluationFilter(
                        gameId = gameId,
                        evaluatorUsername = actor.subject,
                        from = from,
                        to = to,
                    )
                    val evaluations = coachingService.findAll(filter).map { it.toResponseDto() }
                    call.respond(HttpStatusCode.OK, evaluations)
                }

                get("/all") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE_COACH_ADMIN) ?: return@get
                    val gameId = call.request.queryParameters["gameId"]
                    val evaluatorUsername = call.request.queryParameters["evaluatorUsername"]
                    val from = call.request.queryParameters["from"]
                    val to = call.request.queryParameters["to"]
                    val filter = CoachingEvaluationFilter(
                        gameId = gameId,
                        evaluatorUsername = evaluatorUsername,
                        from = from,
                        to = to,
                    )
                    val evaluations = coachingService.findAll(filter).map { it.toResponseDto() }
                    call.respond(HttpStatusCode.OK, evaluations)
                }

                get {
                    if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE_COACH_ADMIN) == null) return@get
                    val gameId = call.request.queryParameters["gameId"]
                    val evaluatorUsername = call.request.queryParameters["evaluatorUsername"]
                    val from = call.request.queryParameters["from"]
                    val to = call.request.queryParameters["to"]
                    val evaluations = coachingService.findAll(
                        filter = CoachingEvaluationFilter(
                            gameId = gameId,
                            evaluatorUsername = evaluatorUsername,
                            from = from,
                            to = to,
                        )
                    )
                    call.respond(HttpStatusCode.OK, evaluations.map { it.toResponseDto() })
                }

                get("/{id}") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE_COACH, AuthRole.REFEREE_COACH_ADMIN) ?: return@get
                    val id = call.parameters["id"] ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "id is required")
                    val evaluation = coachingService.findById(id)
                    if (evaluation == null) {
                        call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Evaluation not found")
                        return@get
                    }
                    if (!canAccessEvaluation(actor.role, actor.subject, evaluation.evaluatorUsername)) {
                        call.respondProblem(HttpStatusCode.Forbidden, "Forbidden", "You may not access this evaluation.")
                        return@get
                    }
                    call.respond(HttpStatusCode.OK, evaluation.toResponseDto())
                }

                put("/{id}") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE_COACH, AuthRole.REFEREE_COACH_ADMIN) ?: return@put
                    val id = call.parameters["id"] ?: return@put call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "id is required")
                    val request = call.receive<de.exhumedo.kmp.handball_support.api.dto.coaching.CreateCoachingEvaluationRequestDto>()
                    val existing = coachingService.findById(id)
                    if (existing == null) {
                        call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Evaluation not found")
                        return@put
                    }
                    if (!canModifyEvaluation(actor.role, actor.subject, existing.evaluatorUsername)) {
                        call.respondProblem(HttpStatusCode.Forbidden, "Forbidden", "You may only update your own evaluations.")
                        return@put
                    }
                    if (actor.role != AuthRole.ADMIN && actor.role != AuthRole.REFEREE_COACH_ADMIN && actor.subject != request.evaluatorUsername.trim()) {
                        call.respondProblem(HttpStatusCode.Forbidden, "Forbidden", "You may not change the evaluator of this evaluation.")
                        return@put
                    }
                    val updated = coachingService.update(id, request.toCommand())
                    if (updated == null) {
                        call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Evaluation not found")
                    } else {
                        call.respond(HttpStatusCode.OK, updated.toResponseDto())
                    }
                }

                delete("/{id}") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE_COACH, AuthRole.REFEREE_COACH_ADMIN) ?: return@delete
                    val id = call.parameters["id"] ?: return@delete call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "id is required")
                    val existing = coachingService.findById(id)
                    if (existing == null) {
                        call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Evaluation not found")
                        return@delete
                    }
                    if (!canModifyEvaluation(actor.role, actor.subject, existing.evaluatorUsername)) {
                        call.respondProblem(HttpStatusCode.Forbidden, "Forbidden", "You may only delete your own evaluations.")
                        return@delete
                    }
                    coachingService.delete(id)
                    call.respond(HttpStatusCode.NoContent)
                }

                get("/{id}/report") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE_COACH, AuthRole.REFEREE_COACH_ADMIN) ?: return@get
                    val id = call.parameters["id"] ?: return@get call.respondProblem(HttpStatusCode.BadRequest, "Invalid Request", "id is required")
                    val report = coachingService.buildReport(id)
                    if (report == null) {
                        call.respondProblem(HttpStatusCode.NotFound, "Not Found", "Evaluation not found")
                        return@get
                    }
                    if (!canAccessEvaluation(actor.role, actor.subject, report.evaluatorUsername)) {
                        call.respondProblem(HttpStatusCode.Forbidden, "Forbidden", "You may not access this report.")
                        return@get
                    }
                    call.respond(HttpStatusCode.OK, report.toResponseDto())
                }
            }
        }
    }
}

private fun canAccessEvaluation(role: AuthRole, actorUsername: String, evaluatorUsername: String): Boolean {
    return role == AuthRole.ADMIN || role == AuthRole.REFEREE_COACH_ADMIN || actorUsername.equals(evaluatorUsername, ignoreCase = true)
}

private fun canModifyEvaluation(role: AuthRole, actorUsername: String, evaluatorUsername: String): Boolean {
    return role == AuthRole.ADMIN || role == AuthRole.REFEREE_COACH_ADMIN || actorUsername.equals(evaluatorUsername, ignoreCase = true)
}
