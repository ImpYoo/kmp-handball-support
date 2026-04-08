package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.Greeting
import de.exhumedo.kmp.handball_support.application.PerformanceEvaluationApplicationService
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.api.dto.CreatePerformanceEvaluationRequestDto
import de.exhumedo.kmp.handball_support.api.dto.toCreateCommand
import de.exhumedo.kmp.handball_support.api.dto.toResponseDto
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluatorReference
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.authorize
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Configures API routes for performance evaluations.
 *
 * @param repository Domain repository adapter.
 * @param applicationService Application service handling evaluation creation.
 * @param tokenService Token service used to verify bearer tokens.
 */
fun Application.configureRouting(
    repository: PerformanceEvaluationRepository,
    applicationService: PerformanceEvaluationApplicationService,
    tokenService: JwtTokenService,
    authUserStore: AuthUserStore,
) {
    routing {
        get("/") {
            call.respond(HttpStatusCode.OK, "Ktor: ${Greeting().greet()}")
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }

        route("/api/performance-evaluations") {
            post {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE) == null) return@post
                val request = call.receive<CreatePerformanceEvaluationRequestDto>()
                val command = request.toCreateCommand()
                val saved = applicationService.create(
                    game = command.game,
                    evaluator = command.evaluator,
                    tableOfficialTeam = command.tableOfficialTeam,
                    score = command.score,
                    comment = command.comment,
                )

                call.respond(HttpStatusCode.Created, saved.toResponseDto())
            }

            get {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                val gameId = call.request.queryParameters["gameId"]
                val firstRefereeId = call.request.queryParameters["firstRefereeId"]
                val secondRefereeId = call.request.queryParameters["secondRefereeId"]

                when {
                    gameId != null -> {
                        val evaluations = repository.findByGameId(gameId)
                        call.respond(HttpStatusCode.OK, evaluations.map { it.toResponseDto() })
                    }

                    firstRefereeId != null && secondRefereeId != null -> {
                        val evaluations = repository.findByEvaluatorReference(
                            EvaluatorReference.RefereeTeam(
                                firstRefereeId = firstRefereeId,
                                secondRefereeId = secondRefereeId,
                            ),
                        )
                        call.respond(HttpStatusCode.OK, evaluations.map { it.toResponseDto() })
                    }

                    firstRefereeId != null || secondRefereeId != null -> {
                        call.respondProblem(
                            status = HttpStatusCode.BadRequest,
                            title = "Invalid Request",
                            detail = "Both firstRefereeId and secondRefereeId are required together.",
                        )
                    }

                    else -> {
                        val evaluations = repository.findAll()
                        call.respond(HttpStatusCode.OK, evaluations.map { it.toResponseDto() })
                    }
                }
            }

            get("/{id}") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                val id = call.parameters["id"]
                    ?: return@get call.respondProblem(
                        status = HttpStatusCode.BadRequest,
                        title = "Invalid Request",
                        detail = "Path parameter 'id' is required.",
                    )

                val evaluation = repository.findById(id)
                if (evaluation == null) {
                    call.respondNotFound("Evaluation '$id' was not found.")
                } else {
                    call.respond(HttpStatusCode.OK, evaluation.toResponseDto())
                }
            }
        }
    }
}

/**
 * Responds with a standardized not-found problem payload.
 *
 * @param detail Human-readable detail for the missing resource.
 */
private suspend fun io.ktor.server.application.ApplicationCall.respondNotFound(detail: String) {
    respondProblem(
        status = HttpStatusCode.NotFound,
        title = "Not Found",
        detail = detail,
    )
}
