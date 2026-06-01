package de.exhumedo.kmp.handball_support.api
import de.exhumedo.kmp.handball_support.application.PerformanceEvaluationApplicationService
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.api.dto.CreatePerformanceEvaluationRequestDto
import de.exhumedo.kmp.handball_support.api.dto.toCreateCommand
import de.exhumedo.kmp.handball_support.api.dto.toResponseDto
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.authorize
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
fun Application.configureRouting(
    repository: PerformanceEvaluationRepository,
    applicationService: PerformanceEvaluationApplicationService,
    tokenService: JwtTokenService,
    authUserStore: AuthUserStore,
) {
    routing {
        // ── PoC UI ─────────────────────────────────────────────────────────────
        get("/ui") { call.respondRedirect("/ui/ui.html", permanent = false) }
        staticResources("/ui", "static")

        get("/") { call.respond(HttpStatusCode.OK, mapOf("name" to "Handball Support API", "version" to "1.0.0")) }
        get("/health") {
            try {
                repository.findAll() // verify the persistence layer is readable
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "DOWN", "detail" to (e.message ?: "persistence check failed")))
            }
        }
        route("/api/performance-evaluations") {
            post {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE) == null) return@post
                val cmd = call.receive<CreatePerformanceEvaluationRequestDto>().toCreateCommand()
                val saved = applicationService.create(cmd.game, cmd.evaluator, cmd.tableOfficialTeam, cmd.score, cmd.comment)
                call.respond(HttpStatusCode.Created, saved.toResponseDto())
            }
            get {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                val gameId = call.request.queryParameters["gameId"]
                val r1 = call.request.queryParameters["firstRefereeId"]
                val r2 = call.request.queryParameters["secondRefereeId"]
                val delegateId = call.request.queryParameters["delegateId"]
                when {
                    gameId != null -> call.respond(HttpStatusCode.OK, repository.findByGameId(gameId).map { it.toResponseDto() })
                    r1 != null && r2 != null -> call.respond(HttpStatusCode.OK, repository.findByRefereeTeam(r1, r2).map { it.toResponseDto() })
                    r1 != null || r2 != null -> call.respondProblem(HttpStatusCode.BadRequest,"Invalid Request","Both firstRefereeId and secondRefereeId are required together.")
                    delegateId != null -> call.respond(HttpStatusCode.OK, repository.findByDelegate(delegateId).map { it.toResponseDto() })
                    else -> call.respond(HttpStatusCode.OK, repository.findAll().map { it.toResponseDto() })
                }
            }
            get("/{id}") {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@get
                val id = call.parameters["id"] ?: return@get call.respondProblem(HttpStatusCode.BadRequest,"Invalid Request","id is required")
                val ev = repository.findById(id)
                if (ev == null) call.respondProblem(HttpStatusCode.NotFound,"Not Found","Evaluation not found") else call.respond(HttpStatusCode.OK, ev.toResponseDto())
            }
        }
    }
}
private suspend fun io.ktor.server.application.ApplicationCall.respondNotFound(detail: String) = respondProblem(io.ktor.http.HttpStatusCode.NotFound,"Not Found",detail)
