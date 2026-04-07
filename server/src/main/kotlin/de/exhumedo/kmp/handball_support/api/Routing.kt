package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.Greeting
import de.exhumedo.kmp.handball_support.application.PerformanceEvaluationApplicationService
import de.exhumedo.kmp.handball_support.api.dto.CreatePerformanceEvaluationRequestDto
import de.exhumedo.kmp.handball_support.api.dto.toCreateCommand
import de.exhumedo.kmp.handball_support.api.dto.toResponseDto
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
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
 */
fun Application.configureRouting(
    repository: PerformanceEvaluationRepository,
    applicationService: PerformanceEvaluationApplicationService,
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
                val request = call.receive<CreatePerformanceEvaluationRequestDto>()
                val command = request.toCreateCommand()
                val saved = applicationService.create(
                    game = command.game,
                    refereePair = command.refereePair,
                    tableOfficialTeam = command.tableOfficialTeam,
                    score = command.score,
                    comment = command.comment,
                )

                call.respond(HttpStatusCode.Created, saved.toResponseDto())
            }

            get {
                val gameId = call.request.queryParameters["gameId"]
                val firstRefereeId = call.request.queryParameters["firstRefereeId"]
                val secondRefereeId = call.request.queryParameters["secondRefereeId"]

                when {
                    gameId != null -> {
                        val evaluation = repository.findByGameId(gameId)
                        if (evaluation == null) {
                            call.respondNotFound("No evaluation found for game '$gameId'.")
                        } else {
                            call.respond(HttpStatusCode.OK, evaluation.toResponseDto())
                        }
                    }

                    firstRefereeId != null && secondRefereeId != null -> {
                        val evaluations = repository.findByRefereePairPersonIds(firstRefereeId, secondRefereeId)
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

private suspend fun io.ktor.server.application.ApplicationCall.respondNotFound(detail: String) {
    respondProblem(
        status = HttpStatusCode.NotFound,
        title = "Not Found",
        detail = detail,
    )
}
